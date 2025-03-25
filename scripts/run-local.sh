#!/bin/bash

# Script para executar a aplicação localmente

# Entrar no diretório raiz do projeto (ajuste se necessário)
cd $(dirname $0)/..

# Verificar se o banco de dados está rodando
if ! docker ps | grep -q "postgres"; then
  echo "Iniciando o banco de dados PostgreSQL..."
  docker run --name rinha-postgres -e POSTGRES_PASSWORD=123 -e POSTGRES_USER=admin -e POSTGRES_DB=rinha -p 5432:5432 -d postgres:latest

  # Aguardar inicialização do banco
  echo "Aguardando inicialização do banco de dados..."
  sleep 5

  # Executar o script de inicialização
  echo "Inicializando banco de dados..."
  docker cp docker/db/init.sql rinha-postgres:/docker-entrypoint-initdb.d/init.sql
  docker exec -it rinha-postgres psql -U admin -d rinha -f /docker-entrypoint-initdb.d/init.sql
fi

# Construir o projeto
echo "Construindo o projeto..."
./gradlew clean shadowJar

# Executar a aplicação
echo "Executando a aplicação..."
java --enable-preview -jar build/libs/rinha-backend.jar