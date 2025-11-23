# Deploy & Publicação

Este documento descreve passos para empacotar a aplicação em Docker, publicar numa registry (Docker Hub, GHCR, ECR) e rodar em um ambiente com `docker-compose`. Também inclui opções rápidas para Cloud Run (GCP) e ECS (AWS).

## Pré-requisitos
- Ter o jar gerado: `mvn -DskipTests package` (arquivo em `target/untitled-1.0-SNAPSHOT.jar`)
- Docker instalado e logado (`docker login`)
- (Opcional) Conta em Docker Hub / GHCR / ECR
- Variáveis de ambiente configuradas para o ambiente de produção

## Variáveis de ambiente importantes
Defina as variáveis no ambiente ou usando um `.env` para o `docker-compose`:

- `APP_JWT_SECRET` — segredo para tokens JWT (ex: changeit...) 
- `APP_JWT_EXPIRATION_MS` — tempo de expiração em ms
- `SPRING_DATASOURCE_URL` — JDBC URL do banco
- `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`
- `OPENAI_API_KEY` — chave OpenAI (se aplicável)
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` — RabbitMQ
- (Opcional) `REDIS_HOST`, `REDIS_PORT` para cache em produção

## Docker build & push (exemplo Docker Hub)
1. Ajuste as variáveis:
```bash
export DOCKER_REGISTRY=docker.io
export DOCKER_REPO=<seu-usuario>/smartleader
export IMAGE_TAG=latest
```
2. Build da imagem:
```bash
docker build -t ${DOCKER_REPO}:${IMAGE_TAG} .
```
3. Push para registry:
```bash
docker push ${DOCKER_REPO}:${IMAGE_TAG}
```
4. Rodar com Docker:
```bash
docker run -e APP_JWT_SECRET=${APP_JWT_SECRET} -e SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL} -p 8080:8080 ${DOCKER_REPO}:${IMAGE_TAG}
```

## Usando Docker Compose
1. Ajuste `docker-compose.yml` (se necessário) para incluir serviços: db, rabbitmq, redis.
2. Crie `.env` com as variáveis necessárias.
3. Execute:
```bash
docker-compose up -d --build
```

## Publicar no Google Cloud Run (exemplo rápido)
1. Build e push para Google Container Registry (gcr.io):
```bash
gcloud auth configure-docker
docker build -t gcr.io/PROJECT_ID/smartleader:${IMAGE_TAG} .
docker push gcr.io/PROJECT_ID/smartleader:${IMAGE_TAG}
```
2. Deploy no Cloud Run:
```bash
gcloud run deploy smartleader --image gcr.io/PROJECT_ID/smartleader:${IMAGE_TAG} --platform managed --region us-central1 --allow-unauthenticated --set-env-vars "APP_JWT_SECRET=${APP_JWT_SECRET},SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL},OPENAI_API_KEY=${OPENAI_API_KEY}"
```

## Publicar no AWS ECS (exemplo rápido)
- Geralmente envolve criar um repositório ECR, push da imagem e configurar um Task Definition + Service.
- Recomendo usar `ecs-cli` ou Terraform para infra reprodutível.

## Notas sobre produção
- Nunca coloque segredos no `Dockerfile` — use `secrets`/envvars do provedor.
- Use um `CacheManager` com Redis para performance em produção.
- Configure health checks para o container (ex: `/actuator/health` se expor).  
- Configure logging/monitoring (CloudWatch / Stackdriver / Prometheus).

## Script de exemplo
- Use `scripts/docker-build-push.sh` (fornecido) e ajuste `DOCKER_REPO` e `DOCKER_REGISTRY`.

## Aplicar script SQL no banco
- Antes de rodar em produção, adicione a coluna `ds_fatores` no seu schema com os scripts em `scripts/`.

## Próximos passos recomendados
1. Criar `docker-compose.prod.yml` com serviços: app, db (Postgres), rabbitmq, redis.
2. Adicionar CI (GitHub Actions / GitLab CI) para build, teste e publicar imagens.
3. Teste E2E após deploy.

---

Se quiser, eu gero agora um `docker-compose.prod.yml` de exemplo e o script `scripts/docker-build-push.sh` que automatiza build + push (ex.: Docker Hub). Diga se prefere Docker Hub, GHCR, ou AWS ECR e eu adapto o script automaticamente.


## Azure Pipelines & CI/CD com Self-Hosted Agent

### Visão Geral

Este projeto utiliza **Azure Pipelines** com um **Self-Hosted Agent** para automatizar o processo de build, teste e deployment.

- **Pipeline**: `azure-pipelines.yml` (raiz do repositório)
- **Agent**: Executado em uma VM Azure Linux
- **Trigger**: Toda mudança em `main` ou `master` dispara o pipeline

### Arquitetura da Pipeline

```
┌─────────────────────┐
│  Git Push (main)    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Azure Pipelines   │ ◄─── Dispara automáticamente
└──────────┬──────────┘
           │
           ├──► STAGE 1: BUILD
           │    - Maven clean verify package
           │    - Java 17 setup
           │    - Publish JUnit test results
           │    - Publish artifacts
           │
           └──► STAGE 2: DEPLOY (se BUILD sucesso)
                - Download artifacts
                - Stop aplicação anterior
                - Copy novo JAR
                - Start nova aplicação
                - Health check via /actuator/health
```

### Setup do Self-Hosted Agent

#### Pré-requisitos

- VM Linux (Ubuntu 18.04+) já criada em Azure
- Token PAT (Personal Access Token) do Azure DevOps
- Acesso SSH ou Cloud Shell para a VM

#### Passos de Instalação

**1. Clone o repositório na VM:**

```bash
cd ~
git clone https://github.com/JPAmorimBV/GS--JAVA.git
cd GS--JAVA
```

**2. Faça download do script de setup:**

```bash
chmod +x scripts/setup-azure-agent.sh
```

**3. Execute o script com seu PAT token:**

```bash
./scripts/setup-azure-agent.sh '<SEU_PAT_TOKEN>' 'agent-vm' 'https://dev.azure.com/seu-org'
```

Substituir:
- `<SEU_PAT_TOKEN>`: Seu token PAT do Azure DevOps
- `seu-org`: Sua organização Azure DevOps

**Exemplo com seu token:**
```bash
./scripts/setup-azure-agent.sh '<YOUR_PAT_TOKEN>61e7gF80V7wezlKaWtDyQwvBEVggbFP4b37RXxVS8JQQJ99BKACAAAAAE8413AAASAZDO1mXj' 'agent-vm' 'https://dev.azure.com/seu-org'
```

**4. Verifique o status do agent:**

```bash
sudo systemctl status vsts.agent.Default.agent-vm.service
```

**5. Para ver os logs:**

```bash
sudo journalctl -u vsts.agent.Default.agent-vm.service -f
```

### Configuração das Variáveis de Pipeline

Variáveis definidas em `azure-pipelines.yml`:

```yaml
APP_NAME: 'gs-java-app'           # Nome da aplicação
APP_PORT: '8080'                  # Porta onde a app roda
ARTIFACT_NAME: 'gs-java-app.jar'  # Nome do JAR
MAVEN_CACHE_FOLDER: ...           # Cache do Maven
```

Edite `azure-pipelines.yml` se precisar alterar estes valores.

### Executar o Pipeline Manualmente

1. Vá para https://dev.azure.com/seu-org
2. Selecione o projeto
3. Vá para **Pipelines**
4. Clique em **GS--JAVA**
5. Clique em **Run pipeline**
6. Confirme

### Validar Deployment

Após o pipeline completar com sucesso:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Logs da aplicação
tail -f ~/app/gs-java-app/app.log

# Verificar processo
ps aux | grep gs-java-app
```

### Troubleshooting

#### Agent não conecta

```bash
# Reiniciar o serviço
sudo systemctl restart vsts.agent.Default.agent-vm.service

# Verificar logs
sudo journalctl -u vsts.agent.Default.agent-vm.service -n 50
```

#### Build falha

1. Verifique que Maven está instalado: `mvn -v`
2. Verifique que Java 17 está disponível: `java -version`
3. Limpe o cache Maven: `rm -rf ~/.m2`

#### Deploy falha

1. Verifique porta 8080 está livre: `netstat -tlnp | grep 8080`
2. Verifique espaço em disco: `df -h`
3. Verifique logs da aplicação: `tail -f ~/app/gs-java-app/app.log`

### Próximos Passos

- [ ] Setup Self-Hosted Agent (execute script)
- [ ] Teste o pipeline com push para main
- [ ] Configure alertas de falha no Azure DevOps
- [ ] Implemente approval gates antes de deploy para produção
- [ ] Adicione SonarQube para análise de código
- [ ] Implemente blue-green deployment
