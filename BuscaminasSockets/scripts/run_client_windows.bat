@echo off
cd /d "%~dp0\.."
java -cp out com.icesi.buscaminas.client.BuscaminasClient 127.0.0.1 8080
pause
