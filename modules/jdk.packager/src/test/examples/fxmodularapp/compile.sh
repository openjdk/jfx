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
mkdir -p build/dist/minesweeperfx
mkdir modules

SOURCE=../../apps/MinesweeperFX/src
RESOURCES=../../apps/MinesweeperFX/src/resources
BUILDDIR=`pwd`/build/dist

pushd ${SOURCE} >> /dev/null
$JAVAC $(find . -name "*.java") -d ${BUILDDIR}
popd >> /dev/null

pushd src >> /dev/null
$JAVAC $(find . -name "*.java") -d ${BUILDDIR}
popd >> /dev/null

# Copy resources
cp -a ${RESOURCES} ${BUILDDIR}/minesweeperfx/resources

${JAR} cMf modules/MinesweeperFX.jar -C build/dist .
