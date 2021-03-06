#!/bin/bash

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

echo $(pwd)

REGISTRY_URL="https://index.docker.io/v1/"
AUTH=$(echo -n $DOCKER_LOGIN:$DOCKER_PWD | base64)
# echo "{\"auths\":{\"$REGISTRY_URL\":{\"auth\":\"$AUTH\"}}}" > config.json
echo "{\"auths\":{\"$REGISTRY_URL\":{\"username\":\"$DOCKER_LOGIN\",\"password\":\"$DOCKER_PWD\"}}}" > config.json
# cat config.json

docker image rm -f $DOCKER_LOGIN/spring-boot-demo:latest
# -v ~/.docker/config.json:/kaniko/.docker/config.json:ro
time docker run -ti --rm -v `pwd`:/workspace -v `pwd`/config.json:/kaniko/.docker/config.json:ro --env DOCKER_CONFIG=/kaniko/.docker gcr.io/kaniko-project/executor:latest --verbosity INFO --cache=true --cache-ttl=168h --cache-repo $DOCKER_LOGIN/spring-boot-demo-cache --dockerfile Dockerfile.maven-host-m2-cache --context dir:///workspace/ --destination $DOCKER_LOGIN/spring-boot-demo
rm config.json
#time docker run -ti --rm -v `pwd`:/workspace -v `pwd`/config.json:/kaniko/.docker/config.json:ro --env DOCKER_CONFIG=/kaniko/.docker gcr.io/kaniko-project/executor:latest --verbosity DEBUG --dockerfile Dockerfile.maven-host-m2-cache --context dir:///workspace/ --no-push
#docker run -m500M --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

cd $LAUNCH_DIR
