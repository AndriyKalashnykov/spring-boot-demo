#!/bin/bash

pushd ../
mvn clean package -DskipTests=true -Dmaven.test.skip=true spring-boot:run -Drun.arguments="spring.profiles.active=default"
popd
