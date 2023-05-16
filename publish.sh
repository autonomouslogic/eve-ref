#!/bin/bash -ex

VERSION="$1"

make docker
docker tag autonomouslogic/eve-ref:$VERSION autonomouslogic/eve-ref:latest
docker push autonomouslogic/eve-ref:$VERSION
docker push autonomouslogic/eve-ref:latest
