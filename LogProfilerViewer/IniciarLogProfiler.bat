@echo off
REM 🍰 Desenvolvido por Edison Cake - Fat Jar Builder
REM 🚀 Cria um jar "fat" com FlatLaf embutido para rodar standalone

set LOGFILE=build\log_fatjar.txt
echo ======= LOG DE EXECUÇÃO - %DATE% %TIME% ======= > "%LOGFILE%"

REM Criar pasta build se não existir
if not exist build (
    mkdir build
)

REM Compilar código Java com flatlaf no classpath
echo Compilando com FlatLaf... >> "%LOGFILE%"
javac -source 8 -target 8 -cp "lib\flatlaf-3.4.jar" -d build src\LogProfilerView.java >> "%LOGFILE%" 2>&1
if errorlevel 1 (
    echo ❌ Erro na compilação! Veja o arquivo de log: %LOGFILE%
    start notepad "%LOGFILE%"
    pause
    exit /b 1
)

REM Criar pasta temporária para montagem do fat jar
if exist fatjar_temp rd /s /q fatjar_temp
mkdir fatjar_temp

REM Copiar classes compiladas para pasta temporária
xcopy build\* fatjar_temp\ /E /I /Q

REM Descompactar o conteúdo do flatlaf jar para dentro da pasta temporária
echo Extraindo flatlaf para fat jar... >> "%LOGFILE%"
powershell -command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory('lib\flatlaf-3.4.jar', 'fatjar_temp')" >> "%LOGFILE%" 2>&1
if errorlevel 1 (
    echo ❌ Erro ao extrair flatlaf.jar! Veja o arquivo de log: %LOGFILE%
    start notepad "%LOGFILE%"
    pause
    exit /b 1
)

REM Criar manifest apontando para a classe principal
echo Main-Class: LogProfilerView > fatjar_temp\manifest.txt

REM Criar o fat jar juntando tudo
echo Criando fat jar... >> "%LOGFILE%"
jar cfm build\LogProfilerView.jar fatjar_temp\manifest.txt -C fatjar_temp . >> "%LOGFILE%" 2>&1
if errorlevel 1 (
    echo ❌ Erro ao criar fat jar! Veja o arquivo de log: %LOGFILE%
    start notepad "%LOGFILE%"
    pause
    exit /b 1
)

REM Limpar pasta temporária
rd /s /q fatjar_temp

echo ✅ Fat jar criado com sucesso: build\LogProfilerView-fat.jar
pause
