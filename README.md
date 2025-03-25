# Rinha de Backend Bullet - Java com Virtual Threads (Project Loom)

Este projeto é uma implementação da Rinha de Backend 2024/Q1 utilizando Java 21 com Virtual Threads (Project Loom).

## Tecnologias Utilizadas

- **Java 21**: Utilizando recursos recentes como Virtual Threads (Project Loom)
- **HTTP Server**: Servidor HTTP nativo do Java (com.sun.net.httpserver)
- **PostgreSQL**: Banco de dados relacional
- **Docker e Docker Compose**: Para containerização
- **Nginx**: Como load balancer

## Arquitetura

A solução segue a arquitetura especificada pela Rinha de Backend:

```
              +------------------+
              |                  |
              |     Nginx        |
              |  Load Balancer   |
              |   (porta 9999)   |
              |                  |
              +------------------+
                     / \
                    /   \
                   /     \
      +-----------+       +------------+
      |           |       |            |
      |  API 01   |       |   API 02   |
      |           |       |            |
      +-----------+       +------------+
             \                /
              \              /
               \            /
              +---------------+
              |               |
              |  PostgreSQL   |
              |               |
              +---------------+
```

## Detalhes da Implementação

### Virtual Threads (Project Loom)

O projeto utiliza Virtual Threads (Project Loom) do Java 21, que permite:

- Criação de milhares de threads virtuais com baixo overhead
- Melhor utilização dos recursos do sistema
- Um thread por requisição sem bloquear threads do sistema operacional

```java
// Configuração do servidor para usar Virtual Threads
// server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
```

### Otimizações do PostgreSQL

O PostgreSQL foi configurado com parâmetros para otimizar o desempenho em alta concorrência:

```yaml
command: postgres -c checkpoint_timeout=600 -c max_connections=100 -c shared_buffers=128MB -c synchronous_commit=off -c fsync=off
```

### Configurações do Nginx

O Nginx foi configurado para otimizar o balanceamento de carga e o gerenciamento de conexões:

- `worker_connections 2048`: Aumenta o número de conexões simultâneas
- `keepalive 500`: Mantém conexões persistentes com os servidores API
- Outros parâmetros para melhorar o desempenho (TCP tuning, buffer sizes, etc.)

## Como Executar

### Requisitos

- Docker e Docker Compose
- Java 21 (apenas para desenvolvimento local)

### Passos para Execução

1. Clone o repositório:
   ```bash
   git clone [https://github.com/bulletdev/rinha-backend-bullet.git]
   cd [rinha-backend-bullet]
   ```

2. Execute com Docker Compose:
   ```bash
   docker-compose up -d
   ```

3. A API estará disponível na porta 9999:
   ```bash
   curl -X GET http://localhost:9999/clientes/1/extrato
   ```

## Endpoints da API

### 1. Criar Transação

```
POST /clientes/[id]/transacoes

{
    "valor": 1000,
    "tipo": "c",
    "descricao": "descricao"
}
```

### 2. Consultar Extrato

```
GET /clientes/[id]/extrato
```

## Limitações de Recursos

O projeto respeita as limitações de CPU e memória impostas pela Rinha de Backend:
- Total CPU: 1.5 unidades (distribuídas entre os serviços)
- Total Memória: 550MB (distribuída entre os serviços)

## Melhorias Possíveis

- Implementação de um pool de conexões mais robusto com HikariCP
- Utilização de caching para reduzir a carga no banco de dados
- Otimizações adicionais nas consultas SQL

## Autor

- Michael Bullet
- Contato@michaelbullet.com
- http://hithub.com/bulletdev
- http://michaelbullet.com
```
