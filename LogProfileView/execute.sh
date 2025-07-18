#!/bin/bash

# Caminho do JAR
JAR_FILE="LogProfilerView.jar"

# Verifica se o Java está instalado
if ! command -v java &> /dev/null
then
    echo "Java não encontrado. Por favor, instale o Java (JDK) antes de continuar."
    exit 1
fi

# Verifica se o arquivo .jar existe
if [ ! -f "$JAR_FILE" ]; then
    echo "Arquivo $JAR_FILE não encontrado no diretório atual: $(pwd)"
    exit 1
fi

# Executa o programa
echo "Iniciando LogProfilerView..."
java -jar "$JAR_FILE" &
