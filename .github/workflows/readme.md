# GitHub Actions para Rinha de Backend
 
Este diretório contém os workflows do GitHub Actions para automatizar a compilação, testes e publicação do projeto Rinha de Backend.

## Workflows

### 1. Build and Test (`build-and-test.yml`)

Este workflow é executado automaticamente em cada push para as branches `main` ou `master`, ou quando um pull request é criado para estas branches.

**Etapas:**

1. **Build:**
   - Configura o ambiente Java 21
   - Compila o código usando Gradle
   - Cria o JAR usando a task shadowJar
   - Armazena o JAR como um artefato

2. **Docker:**
   - Baixa o JAR compilado
   - Constrói a imagem Docker
   - Inicia o ambiente com Docker Compose
   - Executa verificações básicas de saúde da API
   - Testa transações e extratos

3. **Performance Test:**
   - Instala k6
   - Inicia o ambiente com Docker Compose
   - Executa testes de carga com k6

### 2. Docker Publish (`docker-publish.yml`)

Este workflow é executado quando uma tag começando com "v" é criada (ex: v1.0.0), ou manualmente.

**Etapas:**

1. Compila o projeto
2. Faz login no Docker Hub
3. Constrói a imagem Docker
4. Publica a imagem no Docker Hub com várias tags:
   - Tag da versão completa (ex: v1.2.3)
   - Tag da versão principal e secundária (ex: 1.2)
   - Tag da branch
   - Tag curta do commit SHA

## Configuração Necessária

Para que o workflow de publicação funcione, você precisa configurar os seguintes secrets no seu repositório:

1. `DOCKERHUB_USERNAME`: Seu nome de usuário do Docker Hub
2. `DOCKERHUB_TOKEN`: Um token de acesso do Docker Hub (não use sua senha)

### Como configurar os secrets:

1. Vá para seu repositório no GitHub
2. Clique em "Settings" > "Secrets and variables" > "Actions"
3. Clique em "New repository secret"
4. Adicione os dois secrets mencionados acima

## Execução Manual

Você pode executar manualmente ambos os workflows usando a opção "Run workflow" na aba "Actions" do GitHub.

## Customização

Sinta-se à vontade para ajustar os workflows às suas necessidades específicas:

- Modificar parâmetros de teste
- Adicionar mais etapas de validação
- Ajustar a estratégia de tags para o Docker
- Adicionar notificações (Slack, Email, etc.)
