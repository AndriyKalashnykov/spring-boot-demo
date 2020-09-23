#!/bin/bash

pushd ../

docker image rm -f spring-boot-demo:latest
docker build  -f Dockerfile.maven-host-m2-cache -t spring-boot-demo .
# docker run -m500M --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

popd