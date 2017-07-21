#!/bin/bash

DEBUG_ARG="-J-Xdebug:";

# Argument parsing.
ARGS=()
for i in "$@"; do
    if [[ "$i" == ${DEBUG_ARG}* ]]; then
        ADDRESS=${i:${#DEBUG_ARG}}
        DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=${ADDRESS}"
        export ANT_OPTS=${DEBUG}
    fi
done

rm -r -f output

ant
