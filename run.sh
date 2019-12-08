#!/usr/bin/env bash

set -e

# Change working directory to the location of this script
cd "$(dirname "$0")"

# Build distribution
./gradlew cerberus-web:bootJar

JAVA_OPT=""

JVM_ARG="-Xdebug -Xrunjdwp:transport=dt_socket,address=5006,server=y,suspend=n"

# Run
java ${JVM_ARG} -jar ${JAVA_OPT} ./cerberus-web/build/libs/cerberus-web.jar "$@"
