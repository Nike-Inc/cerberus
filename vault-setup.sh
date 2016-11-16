#!/bin/bash

ROOT_TOKEN=$1

if [ -z "${ROOT_TOKEN}" ]; then
    echo "Root token parameter is required!"
    exit 1
fi

VAULT_ADDR="http://localhost:8200"
curl -s -X PUT -d '{"rules": "path \"auth/token/lookup\" {policy = \"read\"}"}' -H "X-Vault-Token: ${ROOT_TOKEN}"  http://localhost:8200/v1/sys/policy/lookup-self
VAULT_TOKEN=$(curl -sb -X POST -d '{"display_name":"cerberus-management-service-token"}' -H "X-Vault-Token: ${ROOT_TOKEN}"  http://localhost:8200/v1/auth/token/create | jq -r .auth.client_token)

echo "# CMS Vault Environment Variables:"
echo "export VAULT_ADDR=\"${VAULT_ADDR}\""
echo "export VAULT_TOKEN=\"${VAULT_TOKEN}\""