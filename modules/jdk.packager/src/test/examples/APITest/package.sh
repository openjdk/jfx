#!/bin/bash

if [ -z ${JAVA_HOME} ]; then
  echo "Error: set JAVA_HOME"
  exit
fi

DEBUG_ARG="-J-Xdebug:";
DEBUG=""

# Argument parsing.
ARGS=()
for i in "$@"; do
    if [[ "$i" == ${DEBUG_ARG}* ]]; then
        ADDRESS=${i:${#DEBUG_ARG}}
        DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=${ADDRESS}"
    else
        ARGS+=("\"$i\"")
    fi
done

JAVA=${JAVA_HOME}/bin/java
JAVAC=${JAVA_HOME}/bin/javac
JAVAPACKAGER=${JAVA_HOME}/bin/javapackager
JMODS=${JAVA_HOME}/jmods
JAR=${JAVA_HOME}/bin/jar
MODULES=${JAVA_HOME}/jmods

eval $JAVAC $(IFS=$' '; echo "${ARGS[*]}").java
eval $JAVA $DEBUG $(IFS=$' '; echo "${ARGS[*]}")
