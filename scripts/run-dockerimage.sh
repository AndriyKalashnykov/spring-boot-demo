#!/bin/bash

# set -e
# set -x

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

docker stop spring-boot-demo

# adding 100 to port number to avoid local conflicts (McAfee runs on 8081)
docker run -d --rm --name spring-boot-demo -p 8080:8080 -p 8181:8081 spring-boot-demo:latest &
sleep 15

cd $SCRIPT_DIR
exec ./run-curl.sh

cd $LAUNCH_DIR