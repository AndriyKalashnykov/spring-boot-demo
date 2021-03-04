#!/bin/bash

LAUNCH_DIR=$(pwd); SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd $SCRIPT_DIR; cd ..; SCRIPT_PARENT_DIR=$(pwd);
cd $SCRIPT_PARENT_DIR

mvn clean package -DskipTests=true -Dmaven.test.skip=true spring-boot:run -Drun.arguments="spring.profiles.active=default"

cd $LAUNCH_DIR
