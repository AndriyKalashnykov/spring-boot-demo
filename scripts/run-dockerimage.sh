#!/bin/bash

# set -e
# set -x

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

docker stop spring-boot-demo

# adding 100 to port number to avoid local conflicts: 8081->8181
docker run -d --rm --name spring-boot-demo -p 8080:8080 -p 8181:8081 spring-boot-demo:latest &
echo "Waiting for image to start ..."
sleep 15

echo "Running test script HTTP/POST/GET - ./run-curl.sh"
cd $SCRIPT_DIR
exec ./run-curl.sh

docker stop spring-boot-demo

cd $LAUNCH_DIR