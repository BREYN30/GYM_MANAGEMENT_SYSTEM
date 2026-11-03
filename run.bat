@echo off
cd /d "%~dp0"
echo Starting ENCORE Pro Gym Management System...
set "CP=lib\sqlite-jdbc-3.50.3.0.jar;GMSfinal\lib\sqlite-jdbc-3.50.3.0.jar;GMSfinal\target\classes;target\classes"
java --enable-native-access=ALL-UNNAMED -cp "%CP%" gym.GymApp
if errorlevel 1 (
    "C:\Program Files\Java\jdk-27\bin\java.exe" --enable-native-access=ALL-UNNAMED -cp "%CP%" gym.GymApp
)

