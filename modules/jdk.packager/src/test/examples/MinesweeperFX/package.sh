#!/bin/bash

if [ -z ${JAVA_HOME} ]; then
  echo "Error: set JAVA_HOME"
  exit
fi

PLATFORM=`bash ../whichplatform.sh`
VERSION=1.2
MAC_APPSTORE_ARGS=""

# Argument parsing.
ARGS=()
for i in "$@"; do
  if [[ "$i" == "--MacAppStore" ]]; then
    MAC_APPSTORE_ARGS="-BappVersion=${VERSION} -Bmac.CFBundleIdentifier=com.oratest.minesweeper -Bmac.category=\"public.app-category.games\" -Bmac.CFBundleVersion=${VERSION}"
  else
    ARGS+=("\"$i\"")
  fi
done

echo $MAC_APPSTORE_ARGS

JAVA=${JAVA_HOME}/bin/java
JAVAC=${JAVA_HOME}/bin/javac
JAVAPACKAGER=${JAVA_HOME}/bin/javapackager
JMODS=${JAVA_HOME}/jmods
JAR=${JAVA_HOME}/bin/jar
MODULES=${JAVA_HOME}/jmods

rm -rf output
mkdir output

# Generate ICNS file
ICON="";

if [[ $PLATFORM == "MAC" ]]; then
  mkdir build
  bash ../pngtoicns.sh build/dist/minesweeperfx/resources/MinesweeperFX.png build/MinesweeperFX
  ICON="-Bicon.icns=build/MinesweeperFX.icns"
fi

# Generate app bundle
eval ${JAVAPACKAGER} $(IFS=$' '; echo "${ARGS[*]}") \
  -deploy \
  -v \
  -outdir output \
  -name MinesweeperFX \
  $ICON \
  -native \
  -Bversion=9.0 \
  -Bmac.dmg.simple=true \
  --module MinesweeperFX/minesweeperfx.MinesweeperFX \
  --module-path modules \
  -BapplicationCategory=SomeCategory \
  ${MAC_APPSTORE_ARGS}
