@echo off
echo Starting KyusiyuVenture with software rendering...
cd /d "%~dp0"
mvn exec:java -q
pause