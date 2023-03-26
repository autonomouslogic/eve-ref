#!/bin/bash -e

curl -s https://esi.evetech.net/latest/swagger.json | jq . > esi-latest.json
