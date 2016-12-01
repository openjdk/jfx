#!/bin/bash

PLATFORM=`bash whichplatform.sh`
APP=""

if [[ $PLATFORM == 'WIN' ]]; then
  APP="Test.exe"
elif [[ $PLATFORM == "MAC" ]]; then
  APP="Test.app"
elif [[ $PLATFORM == 'LINUX' ]]; then
  APP="Test"
fi

bash execall.sh $APP
