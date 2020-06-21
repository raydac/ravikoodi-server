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
echo Icon=$RAVIKOODI_HOME/icon.svg >> $TARGET
echo "Categories=Application;" >> $TARGET
echo "Keywords=kodi;ravi;koodi;tv;stream;" >> $TARGET
echo StartupWMClass=RavikoodiServer >> $TARGET
echo StartupNotify=true >> $TARGET

echo Desktop script has been generated: $TARGET

if [ -d ~/.gnome/apps ]; then
    echo copy to ~/.gnome/apps
    cp -f $TARGET ~/.gnome/apps
fi

if [ -d ~/.local/share/applications ]; then
    echo copy to ~/.local/share/applications
    cp -f $TARGET ~/.local/share/applications
fi

if [ -d ~/Desktop ]; then
    echo copy to ~/Desktop
    cp -f $TARGET ~/Desktop
fi

