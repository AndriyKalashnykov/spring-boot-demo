#!/bin/bash

pushd ../

#docker image rm -f spring-boot-demo:latest

docker build  -f Dockerfile -t spring-boot-demo .

docker rm spring-boot-demo
docker run --rm -m500M --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

popd