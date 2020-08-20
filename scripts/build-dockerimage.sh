#!/bin/bash

pushd ../

docker rm -f spring-boot-demo
docker build  -f Dockerfile -t spring-boot-demo .
#docker run -m500M --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

popd