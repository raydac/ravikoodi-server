#!/bin/bash

# Script just generates free desktop descriptor to start application

RAVIKOODI_HOME="$(realpath $(dirname ${BASH_SOURCE[0]}))"
TARGET=$RAVIKOODI_HOME/ravikoodi.desktop

echo [Desktop Entry] > $TARGET
echo Encoding=UTF-8 >> $TARGET
echo Name=RaviKoodi >> $TARGET
echo Comment=Ravikoodi server >> $TARGET
echo GenericName=Ravikoodi >> $TARGET
echo Exec=$RAVIKOODI_HOME/run.sh >> $TARGET
echo Terminal=false >> $TARGET
echo Type=Application >> $TARGET
echo Icon=$RAVIKOODI_HOME/icon.png >> $TARGET
echo Categories=Application; >> $TARGET
echo StartupWMClass=RavikoodiServer >> $TARGET
echo StartupNotify=true >> $TARGET


chmod +x $TARGET

echo Desktop script has been generated: $TARGET
