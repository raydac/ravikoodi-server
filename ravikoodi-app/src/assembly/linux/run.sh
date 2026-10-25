#!/bin/bash

RAVIKOODI_HOME="$(dirname ${BASH_SOURCE[0]})"
JAVA_HOME=$RAVIKOODI_HOME/jre

JAVA_FLAGS="-Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"

JAVA_RUN=$JAVA_HOME/bin/java

if [ -f $RAVIKOODI_HOME/.pid ];
then
    SAVED_PID=$(cat $RAVIKOODI_HOME/.pid)
    if [ -f /proc/$SAVED_PID/exe ];
    then
        echo Application already started! if it is wrong, just delete the .pid file in the application folder root!
        exit 1
    fi
fi

$JAVA_RUN $JAVA_FLAGS -jar "$RAVIKOODI_HOME"/ravikoodi.jar $@
THE_PID=$!
echo $THE_PID>$RAVIKOODI_HOME/.pid
wait $THE_PID
rm $RAVIKOODI_HOME/.pid
exit 0
