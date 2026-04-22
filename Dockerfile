# renovate: datasource=docker depName=maven
ARG MAVEN_IMAGE_VERSION=3.9.15-eclipse-temurin-25

# renovate: datasource=docker depName=eclipse-temurin
ARG TEMURIN_IMAGE_VERSION=25.0.2_10-jre-jammy@sha256:d36843a6f1af5d0aca01ef3d926e2220444eb19fe38e1b23cef3d663ef29b306

FROM maven:${MAVEN_IMAGE_VERSION} AS build
WORKDIR /build
COPY pom.xml .
COPY .git .
RUN mvn dependency:go-offline

COPY ./pom.xml /tmp/
COPY ./src /tmp/src/
COPY ./.git /tmp/.git/

WORKDIR /tmp/
RUN mvn clean package

# extract JAR Layers
WORKDIR /tmp/target
RUN java -Djarmode=layertools -jar *.jar extract

FROM eclipse-temurin:${TEMURIN_IMAGE_VERSION} AS runtime

# Eclipse Temurin 25 LTS (Ubuntu Jammy) — actively CVE-patched upstream.
# Create a non-root user with a numeric UID so Kubernetes can verify
# `runAsNonRoot: true` at admission time.
RUN groupadd -g 65532 -r nonroot \
 && useradd -u 65532 -g nonroot -r -s /usr/sbin/nologin -M nonroot

USER 65532:65532
WORKDIR /application

# copy layers from build image to runtime image as nonroot user
COPY --from=build --chown=65532:65532 /tmp/target/dependencies/ ./
COPY --from=build --chown=65532:65532 /tmp/target/snapshot-dependencies/ ./
COPY --from=build --chown=65532:65532 /tmp/target/spring-boot-loader/ ./
COPY --from=build --chown=65532:65532 /tmp/target/application/ ./

EXPOSE 8080
EXPOSE 8081

ENV _JAVA_OPTIONS="-XX:MinRAMPercentage=60.0 -XX:MaxRAMPercentage=90.0 \
-Djava.security.egd=file:/dev/./urandom \
-Djava.awt.headless=true -Dfile.encoding=UTF-8 \
-Dspring.output.ansi.enabled=ALWAYS \
-Dspring.profiles.active=default"

# Spring Boot 3.2+ relocated JarLauncher to the .launch sub-package
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
