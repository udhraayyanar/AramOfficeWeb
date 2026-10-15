@echo off
if not exist out mkdir out
javac -cp "lib/*" -d out src\*.java
if errorlevel 1 pause & exit /b
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" WebServer
pause
