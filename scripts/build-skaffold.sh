#!/bin/bash
# set -x

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

# https://github.com/GoogleContainerTools/skaffold/tree/master/examples
# https://github.com/GoogleContainerTools/skaffold/blob/master/examples/buildpacks-java/skaffold.yaml

#docker login --username $DOCKER_LOGIN --password $DOCKER_PWD docker.io
#docker image rm -f spring-boot-demo:latest

# skaffold config set --global default-repo <your-docker-hub-username>
skaffold config set default-repo $DOCKER_LOGIN

skaffold build

cd $LAUNCH_DIR