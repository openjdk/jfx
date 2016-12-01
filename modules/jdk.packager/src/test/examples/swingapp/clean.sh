#!/bin/bash

pushd src >> /dev/null
rm *.class
popd >> /dev/null

rm -r -f output
rm -r -f jars
