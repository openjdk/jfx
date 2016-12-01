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

pushd src >> /dev/null

$JAVAC HelloWorld.java

$JAR cmf HelloWorld.manifest hello.world.jar `find . -name '*.class'`

rm -r -f ../jars
mkdir ../jars
mv hello.world.jar ../jars

popd >> /dev/null
