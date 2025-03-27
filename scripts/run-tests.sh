#!/bin/bash

# Script para executar os testes de carga

# Entrar no diretório raiz do projeto (ajuste se necessário)
cd $(dirname $0)/..

# Verificar se o k6 está instalado
if ! command -v k6 &> /dev/null; then
    echo "k6 não está instalado. Instalando..."

    # Tentar instalar k6 baseado no sistema operacional
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        sudo apt-get update
        sudo apt-get install -y gnupg
        sudo gpg -k
        sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
        echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
        sudo apt-get update
        sudo apt-get install -y k6
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        brew install k6
    else
        echo "Sistema operacional não suportado automaticamente."
        echo "Por favor, instale o k6 manualmente: https://k6.io/docs/getting-started/installation/"
        exit 1
    fi
fi

# Verificar se a aplicação está rodando
echo "Verificando se a API está acessível..."
curl -s -o /dev/null -w "%{http_code}" http://localhost:9999/clientes/1/extrato

# shellcheck disable=SC1046
# shellcheck disable=SC1073
# shellcheck disable=SC1073
if [ $? -ne 0 ]; then
    echo "A API não está acessível. Iniciando o ambiente..."

    # Verificar se as imagens Docker já foram construídas
    if [[ "$(docker images -q rinha-backend-loom:latest 2> /dev/null)" == "" ]]; then
        echo "Construindo imagem Docker..."
        ./scripts/build-docker.sh
    fi

    # Iniciar o ambiente
    echo "Iniciando ambiente Docker..."
    docker-compose up -d

    # Aguardar a inicialização
    echo "Aguardando inicialização da API (30 segundos)..."
    sleep 30

    #