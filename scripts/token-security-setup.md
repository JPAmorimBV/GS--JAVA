# PAT Token Security Setup

## CRITICAL: Token Exposure Incident

A Personal Access Token (PAT) was accidentally exposed in the GitHub repository commit history.
**This token MUST be revoked immediately before proceeding with agent setup.**

## Step 1: Revoke the Exposed Token (USER ACTION REQUIRED)

### Quick Steps:
1. Go to: https://dev.azure.com/
2. Click your **profile icon** (top-right)
3. Select **User settings**
4. Click **Personal access tokens**
5. Find the exposed token and click **Revoke**
6. Confirm revocation

### Exposed Token Details
- **Status**: REVOKED (DO NOT USE)
- **Last Known Scope**: Agent Pools (Read & manage)
- **Exposure Date**: Commit b71fe50 (deploy.md)

## Step 2: Create a New PAT Token

### Steps to Create New Token:
1. Still in **Personal access tokens** page
2. Click **+ New Token**
3. Fill in:
   - **Name**: `gs-java-agent`
   - **Organization**: Select your organization
   - **Expiration**: 90 days (recommended)
   - **Scopes**: Check only **Agent Pools** (Read & manage)
4. Click **Create**
5. **COPY THE TOKEN IMMEDIATELY** - it only displays once!

## Step 3: Securely Store New Token

**IMPORTANT**: Never commit the token to any repository!

### Option A: Local File (Recommended for local testing)
```bash
# Create a local-only token file (NOT in git)
echo '<YOUR_NEW_PAT_TOKEN>' > ~/.azure-token
chmod 600 ~/.azure-token
```

### Option B: Environment Variable
```bash
export AZURE_PAT_TOKEN='<YOUR_NEW_PAT_TOKEN>'
```

### Option C: Azure DevOps CLI
```bash
az devops login --org 'https://dev.azure.com/YOUR_ORG'
# When prompted for PAT, paste your new token
```

## Step 4: Setup Self-Hosted Agent

Once you have the new token:

```bash
cd ~/GS--JAVA
chmod +x scripts/setup-azure-agent.sh

# Execute with new token (from .azure-token file)
new_token=$(cat ~/.azure-token)
./scripts/setup-azure-agent.sh "$new_token" 'agent-vm' 'https://dev.azure.com/YOUR_ORG'
```

## Security Best Practices

- [ ] Revoked old token
- [ ] Created new token with 90-day expiration
- [ ] Stored new token in local file (not in repo)
- [ ] Set file permissions to 600 (`chmod 600 ~/.azure-token`)
- [ ] Added `.azure-token` to `.gitignore`
- [ ] Never shared token in Slack/email/chat
- [ ] Agent setup completed successfully
- [ ] Scheduled token rotation every 90 days

## Verification

After setup, verify token is working:

```bash
# Check agent service status
sudo systemctl status vsts.agent.Default.agent-vm.service

# View agent logs
sudo journalctl -u vsts.agent.Default.agent-vm.service -n 20
```

## Token Rotation Schedule

- Create new token every 90 days
- Revoke old token
- Update agent configuration
- Document rotation in team wiki

## References

- [Azure DevOps Token Security](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate)
- [Revoke PAT Documentation](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/admin-revoke-user-pats)
- See `setup-azure-agent.sh` for agent installation script
