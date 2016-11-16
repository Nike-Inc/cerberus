#!/bin/bash

# =========== If you want to override any of the default JVM behavior args you can do so here. 
# You should only change these if you know what you're doing and have performance tested the changes.
# If you leave these blank then reasonable defaults will be chosen for you.
export JVM_MEMORY_ARGS=
export JVM_GC_ARGS=
export JVM_GC_LOGGING_ARGS=
export JVM_HEAP_DUMP_ON_OOM_ARGS=
export JVM_DNS_TTL=

# =========== App-specific args go here. The usual app ID, environment, and eureka args are auto-generated for you. This is here for args that are truly specific to your app.
# Most apps can leave this blank.
export APP_SPECIFIC_JVM_ARGS="-Duser.timezone=UTC -Dfile.encoding=utf-8"