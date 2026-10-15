@echo off
if not exist out mkdir out
javac -cp "lib/*" -d out src\*.java
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" ShowDb %*
pause