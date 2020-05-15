#!/bin/bash

APPLEFOLDER=./src/assembly/mac/Ravikoodi.app/Contents/Resources

rm $APPLEFOLDER/appico.png
rm $APPLEFOLDER/appico.icns

SRCIMAGE=./ravikoodi-logo-256.png

convert -background none -resize 16x16 $SRCIMAGE .pfx_icon_16x16px.png
convert -background none -resize 32x32 $SRCIMAGE .pfx_icon_32x32px.png
convert -background none -resize 48x48 $SRCIMAGE .pfx_icon_48x48px.png
convert -background none -resize 128x128 $SRCIMAGE .pfx_icon_128x128px.png
convert -background none -resize 256x256 $SRCIMAGE .pfx_icon_256x256px.png

png2icns appico.icns .pfx_icon_*px.png

rm .pfx_icon_*px.png
cp ./appico.icns $APPLEFOLDER
cp $SRCIMAGE $APPLEFOLDER/appico.png