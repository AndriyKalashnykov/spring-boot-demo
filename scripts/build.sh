#!/bin/bash

pushd ../

mvn clean install -DskipTests=true -Dmaven.test.skip=true
popd
