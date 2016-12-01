#!/bin/bash
# Script to report the platform.

platform='unknown'
unamestr=`uname`

if [[ "$unamestr" == 'Darwin' ]]; then
   platform='MAC'
elif [[ "$unamestr" == *CYGWIN* ]]; then
   platform='WIN'
elif [[ "$unamestr" == "Linux" ]]; then
   platform='LINUX'
fi

echo ${platform}
