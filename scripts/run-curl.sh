#!/bin/bash

# set -e
# set -x

curl -X POST 'http://localhost:8080/example/v1/hotels' --header 'Content-Type: application/xml' --header 'Accept: application/xml' --data @hotel.xml --stderr -
curl -X POST 'http://localhost:8080/example/v1/hotels' --header 'Content-Type: application/json' --header 'Accept: application/json' --data @hotel.json --stderr -
# curl -X POST 'http://localhost:8080/example/v1/hotels' --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{"name":"Beds R Us","description":"Very basic, small rooms but clean","city":"Santa Ana","rating":2}' --stderr -
# http POST 'http://localhost:8080/example/v1/hotels' < hotel.json

curl -X GET --silent 'http://localhost:8080/example/v1/hotels?page=0&size=10' --header 'Content-Type: application/json' --header 'Accept: application/json' --stderr - 2>&1 | jq .
curl -X GET --silent 'http://localhost:8080/example/v1/hotels?page=0&size=10' --header 'Content-Type: application/xml' --header 'Accept: application/xml' --stderr - 2>&1 | xmllint --format -
# http  'http://localhost:8080/example/v1/hotels?page=0&size=10'

curl -X GET --silent 'http://localhost:8181/health' --stderr - 2>&1 | jq .
