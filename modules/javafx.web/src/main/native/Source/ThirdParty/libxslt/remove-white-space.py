#!/usr/bin/python
import subprocess
import fileinput
import sys
from os.path import exists
# create webkit-updated-files.txt using command   git diff master --name-only
pathFile = open('webkit-updated-files.txt', 'r')
paths = pathFile.readlines()
extensions = ['rc', 'vcxproj', 'filters', 'pl', 'txt', 'java', 'c', 'h', 'cpp', 'hpp', 'cc', 'jsl', 'fxml', 'css', 'm', 'mm', 'frag', 'vert', 'hlsl', 'metal', 'gradle', 'groovy', 'g4', 'stg']
for path in paths:
    filePath = path.strip()
    extension = filePath[filePath.rindex(".") + 1 : ]
    if extension in extensions:
        if exists(filePath):
            print("exists")
            command = "sed -i '' -e's/[[:space:]]*$//' " + filePath
            subprocess.call(command, shell=True)
        else:
            print("DoesNotExist")
    else:
        print("NA")
