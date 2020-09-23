#!/bin/bash

# Cloud Native Buildpacks
# requires Docker to be installed

pushd ../

#docker image rm -f akalashnykov/spring-boot-demo:latest
#docker rmi -f $(docker images | grep '<none>' | awk '{print $3}') 2>/dev/null

mvn clean spring-boot:build-image -DskipTests

docker rm spring-boot-demo
#-m500M
docker run --rm  --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 akalashnykov/spring-boot-demo:latest

popd