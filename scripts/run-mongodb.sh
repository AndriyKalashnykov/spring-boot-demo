#!/bin/bash

pushd ../

docker run -d --rm --name mongodb -p 27017:27017 -e MONGO_INITDB_DATABASE=admin -e MONGO_INITDB_ROOT_USERNAME=mongo-admin -e MONGO_INITDB_ROOT_PASSWORD=mongo-admin-password mongo:4.2.3

popd
