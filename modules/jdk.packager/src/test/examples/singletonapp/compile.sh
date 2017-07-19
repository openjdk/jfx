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
rm -r -f jars
mkdir -p build/dist
mkdir jars

SOURCE=../../apps/MinesweeperFX/src
BUILDDIR=`pwd`/build/dist

echo $BUILDDIR


pushd ${SOURCE} >> /dev/null
$JAVAC $(find . -name "*.java") -d ${BUILDDIR}
popd >> /dev/null

# Copy resources
cp -a ${SOURCE}/resources ${BUILDDIR}/resources

${JAR} cMf jars/MinesweeperFX.jar -C build/dist .
