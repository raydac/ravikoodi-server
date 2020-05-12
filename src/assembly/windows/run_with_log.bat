set "RAVIKOODI_HOME=%cd%"
set "LOG_FILE=%RAVIKOODI_HOME%/console.log"

set JAVA_FLAGS="--add-opens=java.base/java.lang=ALL-UNNAMED"
set JAVA_RUN="java.exe"

echo %%JAVA_RUN%%=%JAVA_RUN% > %LOG_FILE%

echo ------JAVA_VERSION------ >> %LOG_FILE%

%JAVA_RUN% -version 2>> %LOG_FILE%

echo ------------------------ >> %LOG_FILE%

%JAVA_RUN% %JAVA_FLAGS% -Djava.library.path=%RAVIKOODI_HOME% -jar %RAVIKOODI_HOME%/ravikoodi.jar %* 2>> %LOG_FILE%
