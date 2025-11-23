#!/bin/bash
# =====================================
# Azure Pipelines Self-Hosted Agent Setup
# =====================================
# Este script configura um Self-Hosted Agent para Azure Pipelines
# Uso: ./setup-azure-agent.sh <PAT_TOKEN> <AGENT_NAME> <ORG_URL>
#
# Exemplo:
# ./setup-azure-agent.sh 'seu-token-aqui' 'agent-vm' 'https://dev.azure.com/sua-org'

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Validar argumentos
if [ $# -lt 3 ]; then
    echo -e "${RED}Erro: Faltam argumentos${NC}"
    echo -e "${YELLOW}Uso: $0 <PAT_TOKEN> <AGENT_NAME> <ORG_URL>${NC}"
    echo -e "${YELLOW}Exemplo: $0 'token123' 'agent-vm' 'https://dev.azure.com/myorg'${NC}"
    exit 1
fi

PAT_TOKEN="$1"
AGENT_NAME="$2"
ORG_URL="$3"
AGENT_POOL="Default"

echo -e "${GREEN}=== Iniciando setup do Azure Pipelines Agent ===${NC}"
echo "PAT Token: (oculto)"
echo "Agent Name: $AGENT_NAME"
echo "Organization URL: $ORG_URL"
echo "Agent Pool: $AGENT_POOL"
echo ""

# Step 1: Atualizar sistema
echo -e "${YELLOW}[1/6] Atualizando sistema...${NC}"
sudo apt-get update
sudo apt-get install -y curl wget git

# Step 2: Instalar dependências
echo -e "${YELLOW}[2/6] Instalando dependências...${NC}"
sudo apt-get install -y libicu70 liblttng-ust0 libkrb5-3

# Step 3: Criar diretório do agent
echo -e "${YELLOW}[3/6] Criando diretório do agent...${NC}"
AGENT_DIR="$HOME/agent"
mkdir -p "$AGENT_DIR"
cd "$AGENT_DIR"

# Step 4: Download do agent (com retry)
echo -e "${YELLOW}[4/6] Fazendo download do Azure Pipelines Agent...${NC}"
MAX_RETRIES=3
RETRY_COUNT=0
AGENT_VERSION="3.232.1"

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -o vsts-agent.tar.gz -L "https://vstsagentpackage.azureedge.net/agent/${AGENT_VERSION}/linux-x64/vsts-agent-linux-x64-${AGENT_VERSION}.tar.gz" 2>/dev/null; then
        echo -e "${GREEN}Download bem-sucedido${NC}"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            echo -e "${YELLOW}Retry $RETRY_COUNT de $MAX_RETRIES...${NC}"
            sleep 5
        fi
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}Falha ao fazer download do agent após $MAX_RETRIES tentativas${NC}"
    exit 1
fi

# Step 5: Extrair agent
echo -e "${YELLOW}[5/6] Extraindo agent...${NC}"
tar zxf vsts-agent.tar.gz
rm vsts-agent.tar.gz

# Step 6: Configurar agent (sem interação)
echo -e "${YELLOW}[6/6] Configurando agent...${NC}"

# Criar arquivo de resposta para configuração não-interativa
cat > agent_config.txt << EOF
$ORG_URL
$PAT_TOKEN
$AGENT_POOL
$AGENT_NAME
n
n
EOF

# Executar configuração
./config.sh < agent_config.txt

# Configurar agent como serviço
echo -e "${YELLOW}Configurando agent como serviço systemd...${NC}"
sudo ./svc.install.sh
sudo ./svc.start.sh

echo ""
echo -e "${GREEN}=== Setup completo! ===${NC}"
echo -e "${GREEN}Agent '$AGENT_NAME' foi configurado e iniciado com sucesso${NC}"
echo ""
echo "Verifique o status com:"
echo -e "${YELLOW}sudo systemctl status vsts.agent.${AGENT_POOL}.${AGENT_NAME}.service${NC}"
echo ""
echo "Para ver os logs:"
echo -e "${YELLOW}sudo journalctl -u vsts.agent.${AGENT_POOL}.${AGENT_NAME}.service -f${NC}"
