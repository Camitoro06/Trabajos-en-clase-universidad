@echo off
cd /d "%~dp0\.."
java -cp out com.icesi.buscaminas.server.BuscaminasServer 8080
pause
