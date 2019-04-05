#!/bin/sh

java -jar -D@appId=cms -D@environment=local $* -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 build/libs/*.jar
