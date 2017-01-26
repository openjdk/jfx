#!/bin/bash

if [ -z ${JAVA_HOME} ]; then
  echo "Error: set JAVA_HOME"
  exit
fi

PLATFORM=`bash ../whichplatform.sh`
VERSION=1.0
MAC_APPSTORE_ARGS=""

# Argument parsing.
ARGS=()
for i in "$@"; do
  if [[ "$i" == "--MacAppStore" ]]; then
    MAC_APPSTORE_ARGS="-BappVersion=${VERSION} -Bmac.CFBundleIdentifier=com.oratest.ensemble -Bmac.category=\"public.app-category.education\" -Bmac.CFBundleVersion=${VERSION}"
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

mkdir jars
ENSENBLE8FILE=../../../../../../apps/samples/Ensemble8/dist/Ensemble8.jar

if [[ ! -f ${ENSENBLE8FILE} ]]; then
  echo "Error ${ENSENBLE8FILE} does not exists. Build with \gradle :apps\""
  exit
fi

cp ${ENSENBLE8FILE} jars/Ensemble8.jar

if [[ $PLATFORM == "MAC" ]]; then
  mkdir build
  bash ../pngtoicns.sh resources/Ensemble.png build/Ensemble
  ICON="-Bicon.icns=build/Ensemble.icns"
fi

# Generate app bundle
eval ${JAVAPACKAGER} $(IFS=$' '; echo "${ARGS[*]}") \
  -deploy \
  -v \
  -outdir output \
  -name EnsembleFX \
  $ICON \
  -native \
  -Bversion=9.0 \
  -Bmac.dmg.simple=true \
  -srcdir jars \
  -srcfiles Ensemble8.jar \
  -Bclasspath=Ensemble8.jar \
  -BmainJar=Ensemble8.jar \
  -appClass ensemble.EnsembleApp \
  -BapplicationCategory=SomeCategory \
  ${MAC_APPSTORE_ARGS}
