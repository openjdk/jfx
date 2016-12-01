#!/bin/bash

INFILE=$1
OUTFILE=$2

mkdir $OUTFILE.iconset
sips -z 16 16     $INFILE --out $OUTFILE.iconset/icon_16x16.png
sips -z 32 32     $INFILE --out $OUTFILE.iconset/icon_16x16@2x.png
sips -z 32 32     $INFILE --out $OUTFILE.iconset/icon_32x32.png
sips -z 64 64     $INFILE --out $OUTFILE.iconset/icon_32x32@2x.png
sips -z 128 128   $INFILE --out $OUTFILE.iconset/icon_128x128.png
sips -z 256 256   $INFILE --out $OUTFILE.iconset/icon_128x128@2x.png
sips -z 256 256   $INFILE --out $OUTFILE.iconset/icon_256x256.png
sips -z 512 512   $INFILE --out $OUTFILE.iconset/icon_256x256@2x.png
sips -z 512 512   $INFILE --out $OUTFILE.iconset/icon_512x512.png
cp $INFILE $OUTFILE.iconset/icon_512x512@2x.png
iconutil -c icns $OUTFILE.iconset
rm -R $OUTFILE.iconset