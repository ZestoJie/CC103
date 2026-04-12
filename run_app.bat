@echo off
echo Starting KyusiyuVenture with software rendering...
cd /d "%~dp0"
mvnw.cmd javafx:run -q
pause