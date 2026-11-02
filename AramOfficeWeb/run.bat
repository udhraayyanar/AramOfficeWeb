@echo off
rem --- Needs XAMPP MySQL running (XAMPP Control Panel -> MySQL -> Start) ---
if not exist lib mkdir lib
if not exist lib\mariadb-java-client-3.4.1.jar (
    echo Downloading MySQL/MariaDB JDBC driver ^(one time only^)...
    powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing 'https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.4.1/mariadb-java-client-3.4.1.jar' -OutFile 'lib\mariadb-java-client-3.4.1.jar'"
)
if not exist lib\mariadb-java-client-3.4.1.jar (
    echo Could not download the driver. See README.txt step 1.
    pause & exit /b
)
if not exist out mkdir out
javac -cp "lib/*" -d out src\*.java
if errorlevel 1 pause & exit /b
java -cp "out;lib/*" WebServer
pause
