#!/bin/bash

# Script para limpar e recriar o ambiente Docker completamente

echo "======================================================="
echo "Reiniciando todo o ambiente Docker para Rinha de Backend"
echo "======================================================="

# Parar e remover todos os contêineres
echo "[1/7] Parando contêineres em execução..."
docker-compose down -v

# Limpar imagens antigas
echo "[2/7] Removendo imagens antigas..."
docker rmi rinha-backend-loom:latest 2>/dev/null || true

# Verificar se há imagens e contêineres órfãos
echo "[3/7] Verificando imagens e contêineres órfãos..."
docker system prune -f

# Construir a imagem da aplicação
echo "[4/7] Construindo a imagem da aplicação..."
./gradlew clean shadowJar --no-daemon
docker build -t rinha-backend-loom:latest -f docker/api/Dockerfile .

# Iniciar os serviços com Docker Compose
echo "[5/7] Iniciando os serviços..."
docker-compose up -d

# Aguardar a inicialização
echo "[6/7] Aguardando inicialização dos serviços (30 segundos)..."
sleep 30

# Verificar o status dos serviços
echo "[7/7] Verificando status dos serviços..."
docker-compose ps

echo "======================================================="
echo "Teste de conexão com a API:"
curl -v http://localhost:9999/clientes/1/extrato || echo "Falha na conexão"
echo "======================================================="

echo "Logs do PostgreSQL:"
docker-compose logs db | tail -n 20
echo "======================================================="

echo "Logs da API 01:"
docker-compose logs api01 | tail -n 20
echo "======================================================="

echo "Logs da API 02:"
docker-compose logs api02 | tail -n 20
echo "======================================================="

echo "Ambiente Docker reiniciado. Use 'docker-compose logs -f' para acompanhar os logs."