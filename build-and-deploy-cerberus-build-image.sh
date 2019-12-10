#!/usr/bin/env bash

set -e

docker login
docker build -f Dockerfile.build -t cerberusoss/cerberus-build-image:latest .
docker push cerberusoss/cerberus-build-image
