#!/bin/bash

# Script para construir as imagens Docker do projeto

# Entrar no diretório raiz do projeto (ajuste se necessário)
cd $(dirname $0)/..

# Mostrar informações
echo "Construindo a imagem Docker para Rinha de Backend..."

# Build com Gradle primeiro
echo "Executando build com Gradle..."
./gradlew clean shadowJar

# Construir a imagem Docker
echo "Construindo a imagem Docker..."
docker build -t rinha-backend-bullet:latest -f docker/api/Dockerfile .

echo "Imagem construída com sucesso!"
echo "Para executar o projeto, utilize: docker-compose up -d"