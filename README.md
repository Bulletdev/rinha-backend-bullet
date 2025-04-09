[![Build and Test](https://github.com/Bulletdev/rinha-backend-bullet/actions/workflows/build-and-test.yml/badge.svg?branch=master)](https://github.com/Bulletdev/rinha-backend-bullet/actions/workflows/build-and-test.yml)
[![K6](https://img.shields.io/badge/K6%20Test-Passing-green)](https://bulletdev.grafana.net/a/k6-app/runs/4225718?tab=thresholds)  


# Rinha de Backend Bullet - Java com Virtual Threads (Project Loom) 

Este projeto é uma implementação da Rinha de Backend 2024/Q1 utilizando Java 21 com Virtual Threads (Project Loom).

## Tecnologias Utilizadas

- **Java 21**: Utilizando recursos recentes como Virtual Threads (Project Loom)
- **HTTP Server**: Servidor HTTP nativo do Java (com.sun.net.httpserver)
- **HikariCP**: Pool de conexões de alto desempenho
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

// Configuração do servidor para usar Virtual Threads   
```java
server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
```

### Otimizações do Banco de Dados

O PostgreSQL foi configurado com parâmetros para otimizar o desempenho em alta concorrência:

```yaml
command: postgres -c checkpoint_timeout=600 -c max_connections=200 -c shared_buffers=256MB 
  -c synchronous_commit=off -c fsync=off -c work_mem=12MB -c maintenance_work_mem=128MB 
  -c random_page_cost=1.1 -c effective_cache_size=300MB -c max_parallel_workers_per_gather=4 
  -c max_parallel_workers=8 -c max_worker_processes=8
```

Além disso, utilizamos HikariCP para gerenciamento eficiente de conexões com o banco:

// Configurações otimizadas para o pool de conexões

```
config.setMaximumPoolSize(20);
config.setMinimumIdle(10);
config.setConnectionTimeout(30000);
config.addDataSourceProperty("cachePrepStmts", "true");
```

### Configurações do Nginx

O Nginx foi configurado para otimizar o balanceamento de carga e o gerenciamento de conexões:

- `worker_connections 2048`: Aumenta o número de conexões simultâneas
- `keepalive 500`: Mantém conexões persistentes com os servidores API
- `proxy_buffer_size` e `proxy_buffers`: Otimização de buffers
- `tcp_nopush`, `tcp_nodelay`: Otimizações TCP

## Como Executar

### Requisitos

- Docker e Docker Compose
- Java 21 (apenas para desenvolvimento local)

### Passos para Execução

1. Clone o repositório:
   ```bash
   git clone https://github.com/bulletdev/rinha-backend-bullet.git
   cd rinha-backend-bullet
   ```

2. Construa as imagens:
   
   ```bash
   docker-compose build
   ```

4. Execute com Docker Compose:
   ```bash
   docker-compose up -d
   ```

5. A API estará disponível na porta 9999:
   ```bash
   curl -X GET http://localhost:9999/clientes/1/extrato
   ```

## Endpoints da API

### 1. Criar Transação

No Windows (prompt de comando):
```bash
curl -X POST http://localhost:9999/clientes/1/transacoes -H "Content-Type: application/json" -d "{\"valor\": 1000, \"tipo\": \"c\", \"descricao\": \"salario\"}"
```

No Linux/Mac:
```bash

curl -X POST http://localhost:9999/clientes/1/transacoes -H "Content-Type: application/json" -d '{"valor": 1000, "tipo": "c", "descricao": "salario"}'
```
### 2. Consultar Extrato

```bash
curl -X GET http://localhost:9999/clientes/1/extrato
```

## Testes de Carga

O projeto inclui um script k6 para testes de carga:

# Se tiver o k6 instalado:
```bash
# Se tiver o k6 instalado:
k6 run k6/script.js
```

## Estrutura do Projeto

O projeto segue uma arquitetura limpa e modular:

```
/src
  /main
    /java/br/com/rinha
      /config      # Configurações (banco de dados, etc.)
      /handler     # Manipuladores de requisições HTTP
      /model       # Entidades do domínio
      /repository  # Acesso a dados
      /util        # Utilitários
```

## Limitações de Recursos

O projeto respeita as limitações de CPU e memória impostas pela Rinha de Backend:
- Total CPU: 1.5 unidades (distribuídas entre os serviços)
- Total Memória: 550MB (distribuída entre os serviços)

## Troubleshooting

Se você encontrar o erro "Connection refused" entre os contêineres, verifique:
1. Se os nomes dos serviços no docker-compose.yml correspondem aos utilizados na configuração
2. Se todos os contêineres estão na mesma rede Docker
3. Se o mapeamento de portas está correto (lembre-se que 9999:9999 é diferente de 9999:80)

## Autor

- Contato@michaelbullet.com
- http://github.com/bulletdev
- http://michaelbullet.com
  ```  
                ⢀⣴⣿⣿⣿⣿⣿⣶⣶⣶⣿⣿⣶⣶⣶⣶⣶⣿⡿⣿⣾⣷⣶⣶⣾⣿⠀                                                                                                                          
             ⣠⣿⣿⢿⣿⣯⠀⢹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⣿⡇⣿⣿⣿⣿⣿⡇                                                                                                         
         ⠀⣰⣿⣿⣷⡟⠤⠟⠁⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢸⡇⣿⣿⣿⣿⣿⡇ 
         ⠀⣿⣿⣿⣿⣿⣷⣶⣿⣿⡟⠁⣮⡻⣿⣿⣿⣿⣿⣿⣿⣿⢸⡇⣿⣿⣿⣿⣿⡇ 
         ⠘⣿⣿⣿⣿⣿⣿⣿⣿⠏⠀⠀⣿⣿⣹⣿⣿⣿⣿⣿⣿⡿⢸⡇⣿⣿⣿⣿⣿⡇ 
         ⠀⠙⢿⣿⣿⣿⡿⠟⠁⣿⣿⣶⣿⠟⢻⣿⣿⣿⣿⣿⣿⡇⣼⡇⣿⣿⣿⣿⣿⠇
         ⠀⠀⠈⠋⠉⠁⣶⣶⣶⣿⣿⣿⣿⢀⣿⣿⣿⣿⣿⣿⣿⣇⣿⢰⣿⣿⣿⣿⣿⠀ 
         ⠀⠀⠀⠀⠀⠙⠿⣿⣿⣿⡄⢀⣠⣾⣿⣿⣿⣿⣿⣿⣿⣽⣿⣼⣿⣿⣿⣿⠇⠀ 
         ⠀⠀⠀⠀⠀⠀⠀⠈⠉⠒⠚⠿⠿⠿⠿⠿⠿⠿⠿⠿⠿⠛⠿⠿⠿⠿⠿⠋⠀⠀ 
         ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀ 
         ⠀⠀⠀⣿⣙⡆⠀⠀⡇⠀⢸⠀⠀⢸⠀⠀ ⢸⡇⠀⠀⢸⣏⡉  ⠙⡏⠁⠀ 
         ⠀⠀⠀⣿⣉⡷⠀⠀⢧⣀⣼ ⠀⢸⣀  ⢸⣇⡀ ⢸⣏⣁⠀ ⠀⡇⠀ 
