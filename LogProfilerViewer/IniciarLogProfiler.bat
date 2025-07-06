@echo off
REM 🍰 Desenvolvido por Edison Cake em 28/06/2025 inicialmente

set LOGFILE=build\log_execucao.txt
echo ======= LOG DE EXECUÇÃO - %DATE% %TIME% ======= > "%LOGFILE%"

REM Criando a pasta build se não existir
if not exist build (
    mkdir build
)

REM Compilando o código-fonte com compatibilidade Java 8
echo Compilando com compatibilidade Java 8... >> "%LOGFILE%"
javac -source 8 -target 8 -d build src\LogProfilerView.java >> "%LOGFILE%" 2>&1
if errorlevel 1 (
    echo ❌ Erro na compilação! Veja o arquivo de log: %LOGFILE%
    start notepad "%LOGFILE%"
    pause
    exit /b 1
)

REM Criando o JAR
echo Criando JAR... >> "%LOGFILE%"
jar cfm build\LogProfilerView.jar manifest.txt -C build . >> "%LOGFILE%" 2>&1
if errorlevel 1 (
    echo ❌ Erro ao criar o JAR! Veja o arquivo de log: %LOGFILE%
    start notepad "%LOGFILE%"
    pause
    exit /b 1
)

REM Executando o programa
echo Executando JAR... >> "%LOGFILE%"
start "" javaw -jar build\LogProfilerView.jar

echo ✅ Tudo finalizado com sucesso! >> "%LOGFILE%"
exit
