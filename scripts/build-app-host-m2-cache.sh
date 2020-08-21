#!/bin/bash

pushd ../

docker run -v ~/.m2:/root/.m2 -v "$PWD":/usr/src -w /usr/src maven:3-jdk-11 mvn clean package -DskipTests=true -Dmaven.test.skip=true

popd
