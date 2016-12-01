#!/bin/bash

PLATFORM=`bash whichplatform.sh`
PREFIX=""

OUTPUT=(`find -L . -name $1 -print`)

for COMMAND in "${OUTPUT[@]}"
do
  if [[ $PLATFORM == "MAC" ]]; then
    if [[ $COMMAND == *".app"* ]]
    then
      PREFIX="open"
    fi
  fi

  if [[ $COMMAND == *".sh"* ]]
  then
    PREFIX="bash"
  fi

  if [[ ! -d ${COMMAND} ]]; then
    echo ${COMMAND}
    DIRECTORY=$(dirname "${COMMAND}")
    pushd $DIRECTORY >> /dev/null
    echo $DIRECTORY
    $PREFIX ./$(basename $COMMAND)
    echo $(basename $COMMAND)
    popd >> /dev/null
  fi
done
