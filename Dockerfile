ARG MVN_VERSION=3.6.3
ARG JDK_VERSION=11

FROM maven:${MVN_VERSION}-jdk-${JDK_VERSION} as build
WORKDIR /build
COPY pom.xml .
COPY .git .
RUN mvn dependency:go-offline

COPY ./pom.xml /tmp/
COPY ./src /tmp/src/
COPY ./.git /tmp/.git/

WORKDIR /tmp/
RUN mvn clean package

FROM gcr.io/distroless/java:${JDK_VERSION}-debug

USER nonroot:nonroot

COPY --from=build --chown=nonroot:nonroot /tmp/target/spring-boot-demo-1.0.jar /spring-boot-demo-1.0.jar

EXPOSE 8080
EXPOSE 8081
EXPOSE 8778
EXPOSE 9779

ENV _JAVA_OPTIONS "-XX:MinRAMPercentage=60.0 -XX:MaxRAMPercentage=90.0 \
-Djava.security.egd=file:/dev/./urandom \
-Djava.awt.headless=true -Dfile.encoding=UTF-8 \
-Dspring.output.ansi.enabled=ALWAYS \
-Dspring.profiles.active=default"

ENTRYPOINT ["java", "-jar", "/spring-boot-demo-1.0.jar"]
