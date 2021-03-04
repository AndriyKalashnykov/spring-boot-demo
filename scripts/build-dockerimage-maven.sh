#!/bin/bash

# Cloud Native Buildpacks
# requires Docker to be installed

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

#docker image rm -f akalashnykov/spring-boot-demo:latest
#docker rmi -f $(docker images | grep '<none>' | awk '{print $3}') 2>/dev/null

mvn clean spring-boot:build-image -DskipTests

docker rm spring-boot-demo
#-m500M
docker run --rm  --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 akalashnykov/spring-boot-demo:latest

cd $LAUNCH_DIR