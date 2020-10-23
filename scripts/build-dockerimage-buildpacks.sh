#!/bin/bash

# https://buildpacks.io/docs/tools/pack/cli/install/
# brew install buildpacks/tap/pack

pushd ../

docker image rm -f spring-boot-demo:latest
docker rmi -f $(docker images | grep '<none>' | awk '{print $3}') 2>/dev/null

# pack inspect-builder gcr.io/buildpacks/builder:v1
#pack inspect-builder gcr.io/paketo-buildpacks/builder:base

# https://github.com/paketo-buildpacks/spring-boot
# https://github.com/spring-cloud/spring-cloud-bindings

# pack build spring-boot-demo --builder=gcr.io/buildpacks/builder:v1 --path=.
# pack build spring-boot-demo --builder gcr.io/paketo-buildpacks/builder:base-platform-api-0.3
pack build spring-boot-demo --builder=gcr.io/paketo-buildpacks/builder:base --path=.

#pack build spring-boot-demo --builder gcr.io/buildpacks/builder:v1 --path=.

docker run --rm --name spring-boot-demo -p 8080:8080 -p 8181:8081 -p 8778:8778 spring-boot-demo:latest

popd