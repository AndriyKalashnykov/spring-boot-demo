#!/bin/bash

# set -e
# set -x

docker stop spring-boot-demo

# adding 100 to port number to avoid local conflicts (McAfee runs on 8081)
docker run -d --rm --name spring-boot-demo -p 8080:8080 -p 8181:8081 spring-boot-demo:latest &
sleep 15

exec ./run-curl.sh
