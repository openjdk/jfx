#!/bin/bash

if [ -z ${JAVA_HOME} ]; then
  echo "Error: set JAVA_HOME"
  exit
fi

# Argument parsing.
ARGS=()
for i in "$@"; do
    ARGS+=("\"$i\"")
done

JAVA=${JAVA_HOME}/bin/java
JAVAC=${JAVA_HOME}/bin/javac
JAVAPACKAGER=${JAVA_HOME}/bin/javapackager
JMODS=${JAVA_HOME}/jmods
JAR=${JAVA_HOME}/bin/jar
MODULES=${JAVA_HOME}/jmods

echo "javapackager=${JAVAPACKAGER}"

rm -r -f output

eval $JAVAPACKAGER $(IFS=$' '; echo "${ARGS[*]}") \
  -deploy -v \
  -outdir output \
  -name Test \
  -Bclasspath=hello.world.jar \
  -native image \
  -BsignBundle=false \
  -BappVersion=9.0 \
  -Bmac.dmg.simple=true \
  -srcdir jars \
  -srcfiles hello.world.jar \
  -appClass HelloWorld \
  -BmainJar=hello.world.jar \
  --strip-native-commands false
