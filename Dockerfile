ARG MVN_VERSION=3.9.9
ARG JDK_VERSION=11

# Docker Hub's maven images moved from the -jdk-N tag family to
# -eclipse-temurin-N; use the Temurin flavor which is the current
# convention and matches the project's mise-managed Temurin JDK.
FROM maven:${MVN_VERSION}-eclipse-temurin-${JDK_VERSION} AS build
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

FROM gcr.io/distroless/java:${JDK_VERSION}-debug AS runtime

# Numeric UID (distroless nonroot = 65532) — lets Kubernetes verify
# `runAsNonRoot: true` at admission time, which a named user cannot.
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

# set entrypoint to layered Spring Boot application
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]