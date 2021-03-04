#!/bin/bash

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

#docker image rm -f spring-boot-demo:latest
#docker rm spring-boot-demo

DOCKER_BUILDKIT=1 docker build -f Dockerfile -t spring-boot-demo .

docker run --rm -m500M --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

cd $LAUNCH_DIR