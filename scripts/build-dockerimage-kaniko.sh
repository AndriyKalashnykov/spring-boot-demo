#!/bin/bash

pushd ../

echo $(pwd)

REGISTRY_URL="https://index.docker.io/v1/"
AUTH=$(echo -n $DOCKER_LOGIN:$DOCKER_PWD | base64)
# echo "{\"auths\":{\"$REGISTRY_URL\":{\"auth\":\"$AUTH\"}}}" > config.json
echo "{\"auths\":{\"$REGISTRY_URL\":{\"username\":\"$DOCKER_LOGIN\",\"password\":\"$DOCKER_PWD\"}}}" > config.json
# cat config.json

docker rm -f spring-boot-demo
# -v ~/.docker/config.json:/kaniko/.docker/config.json:ro
docker run -ti --rm -v `pwd`:/workspace -v `pwd`/config.json:/kaniko/.docker/config.json:ro --env DOCKER_CONFIG=/kaniko/.docker gcr.io/kaniko-project/executor:latest --dockerfile Dockerfile --destination andriykalashnykov/spring-boot-demo --context dir:///workspace/

#docker run -m500M --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

popd