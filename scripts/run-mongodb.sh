#!/bin/bash

# https://github.com/bitnami/bitnami-docker-mongodb
# https://medium.com/faun/managing-mongodb-on-docker-with-docker-compose-26bf8a0bbae3
# https://severalnines.com/database-blog/deploying-mongodb-using-docker

pushd ../

docker run -d --rm --name mongodb -p 27017-27019:27017-27019 -e MONGO_INITDB_DATABASE=admin -e MONGO_INITDB_ROOT_USERNAME=mongo-admin -e MONGO_INITDB_ROOT_PASSWORD=mongo-admin-password mongo:4.2.3

# docker exec -it mongodb bash

# mongo admin --host localhost --authenticationDatabase admin -u mongo-admin -p mongo-admin-password
# show dbs
# use admin
# db.people.save({ firstname: "Nic", lastname: "Raboy" })
# db.people.save({ firstname: "Maria", lastname: "Raboy" })
# db.people.find({ firstname: "Nic" })

# docker stop mongodb

popd || exit
