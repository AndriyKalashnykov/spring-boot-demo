# syntax=docker/dockerfile:1

# renovate: datasource=docker depName=maven
ARG MAVEN_IMAGE_VERSION=3.9.16-eclipse-temurin-25@sha256:1b1fc6d0168ea616afd1c861d6f32ec37c9ec2ffe88a0351b3771dd4ad86b0d8

# renovate: datasource=docker depName=eclipse-temurin
ARG TEMURIN_IMAGE_VERSION=25.0.3_9-jre-jammy@sha256:5bd5dbe00f40ea149de434a75029713765a2912cfc1fd770cc7c7aff007384ea

FROM maven:${MAVEN_IMAGE_VERSION} AS build
WORKDIR /build

# Dependency layer — BuildKit cache mount persists ~/.m2 across builds.
# Invalidates only when pom.xml changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

# Source + git metadata — git-commit-id-plugin needs .git for git.properties.
COPY src src
COPY .git .git
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package

# Extract the Spring Boot layered jar. Spring Boot 4.x removed the old
# `layertools` jarmode; `tools ... extract --layers --launcher` produces the
# same dependencies/spring-boot-loader/snapshot-dependencies/application
# layout the runtime stage + JarLauncher ENTRYPOINT depend on.
WORKDIR /build/target
RUN java -Djarmode=tools -jar ./*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:${TEMURIN_IMAGE_VERSION} AS runtime

# Patch the base image's stale system libs (the base lags security updates
# between rebuilds). Clears fixable Ubuntu CVEs that Trivy gates on — e.g.
# libssl3/openssl 3.0.2-0ubuntu1.23 → 3.0.2-0ubuntu1.25 (CVE-2026-45447,
# Heap UAF in PKCS7_verify). Run as root before the USER switch.
RUN apt-get update \
 && apt-get upgrade -y \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Non-root numeric UID — Kubernetes can verify runAsNonRoot at admission.
RUN groupadd -g 65532 -r nonroot \
 && useradd -u 65532 -g nonroot -r -s /usr/sbin/nologin -M nonroot

USER 65532:65532
WORKDIR /application

# --link decouples these layers from the build-stage digest; rebuilds of
# the build stage don't invalidate the runtime layers when content is
# unchanged.
COPY --link --from=build --chown=65532:65532 /build/target/extracted/dependencies/ ./
COPY --link --from=build --chown=65532:65532 /build/target/extracted/snapshot-dependencies/ ./
COPY --link --from=build --chown=65532:65532 /build/target/extracted/spring-boot-loader/ ./
COPY --link --from=build --chown=65532:65532 /build/target/extracted/application/ ./

EXPOSE 8080

ENV _JAVA_OPTIONS="-XX:MinRAMPercentage=60.0 -XX:MaxRAMPercentage=90.0 \
-Djava.security.egd=file:/dev/./urandom \
-Djava.awt.headless=true -Dfile.encoding=UTF-8 \
-Dspring.output.ansi.enabled=ALWAYS \
-Dspring.profiles.active=default"

# Spring Boot 3.2+ relocated JarLauncher to the .launch sub-package
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
