#!/bin/bash

if [ -z ${JAVA_HOME} ]; then
  echo "Error: set JAVA_HOME"
  exit
fi

JAVA=${JAVA_HOME}/bin/java
JAVAC=${JAVA_HOME}/bin/javac
JAVAPACKAGER=${JAVA_HOME}/bin/javapackager
JMODS=${JAVA_HOME}/jmods
JAR=${JAVA_HOME}/bin/jar
MODULES=${JAVA_HOME}/jmods

rm -r -f build
rm -r -f modules
mkdir -p build/hello.world
mkdir modules

$JAVAC -d build/hello.world --source-path src `find src -name '*.java'`
$JAR --create --file=modules/hello.world.jar --main-class=com.greetings.HelloWorld -C build/hello.world .
