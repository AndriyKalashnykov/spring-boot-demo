# syntax=docker/dockerfile:1

# renovate: datasource=docker depName=maven
ARG MAVEN_IMAGE_VERSION=3.9.15-eclipse-temurin-25

# renovate: datasource=docker depName=eclipse-temurin
ARG TEMURIN_IMAGE_VERSION=25.0.3_9-jre-jammy@sha256:0df1bb22182727e325476c0a9ab38ec4d2b042cbce0ea18a7da71284fea0c40c

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

# Extract Spring Boot layered jar
WORKDIR /build/target
RUN java -Djarmode=layertools -jar ./*.jar extract

FROM eclipse-temurin:${TEMURIN_IMAGE_VERSION} AS runtime

# Non-root numeric UID — Kubernetes can verify runAsNonRoot at admission.
RUN groupadd -g 65532 -r nonroot \
 && useradd -u 65532 -g nonroot -r -s /usr/sbin/nologin -M nonroot

USER 65532:65532
WORKDIR /application

# --link decouples these layers from the build-stage digest; rebuilds of
# the build stage don't invalidate the runtime layers when content is
# unchanged.
COPY --link --from=build --chown=65532:65532 /build/target/dependencies/ ./
COPY --link --from=build --chown=65532:65532 /build/target/snapshot-dependencies/ ./
COPY --link --from=build --chown=65532:65532 /build/target/spring-boot-loader/ ./
COPY --link --from=build --chown=65532:65532 /build/target/application/ ./

EXPOSE 8080

ENV _JAVA_OPTIONS="-XX:MinRAMPercentage=60.0 -XX:MaxRAMPercentage=90.0 \
-Djava.security.egd=file:/dev/./urandom \
-Djava.awt.headless=true -Dfile.encoding=UTF-8 \
-Dspring.output.ansi.enabled=ALWAYS \
-Dspring.profiles.active=default"

# Spring Boot 3.2+ relocated JarLauncher to the .launch sub-package
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
