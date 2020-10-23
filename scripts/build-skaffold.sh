#!/bin/bash

# https://github.com/GoogleContainerTools/skaffold/tree/master/examples
# https://github.com/GoogleContainerTools/skaffold/blob/master/examples/buildpacks-java/skaffold.yaml

pushd ../

#docker image rm -f spring-boot-demo:latest

# skaffold config set --global default-repo <your-docker-hub-username>
skaffold config set default-repo $DOCKER_LOGIN

skaffold build

popd