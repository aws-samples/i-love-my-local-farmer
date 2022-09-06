#!/bin/bash

echo -e "==> Setting variables..."
read -p "Enter the Project Name: " PROJECT
read -p "Enter the Resource Group Name: " RESOURCE_GROUP

echo -e "==> Compressing function source..."
pushd ./src/app
zip -r ../app.zip . -x "*.DS_Store"
popd

echo -e "==> Uploading function source..."
az functionapp deployment source config-zip \
    --name "$PROJECT" \
    --resource-group "$RESOURCE_GROUP" \
    --src ./src/app.zip

echo -e '==> Creating key vault secrets...'

read -p "Enter the Tenant Id: " TENANT_ID
az keyvault secret set --name "x-tenant-id" --value "$TENANT_ID" --vault-name "$PROJECT"

read -p "Enter the Display Name: " DISPLAY_NAME
az keyvault secret set --name "x-display-name" --value "$DISPLAY_NAME" --vault-name "$PROJECT"

read -p "Enter the Application Id: " APPLICATION_ID
az keyvault secret set --name "x-application-id" --value "$APPLICATION_ID" --vault-name "$PROJECT"

read -s -p "Enter the Client Secret: " CLIENT_SECRET
az keyvault secret set --name "x-client-secret" --value "$CLIENT_SECRET" --vault-name "$PROJECT"
#echo ""

echo -e "==> Removing function archive..."
rm -f ./src/app.zip

echo -e "==> Registering resource provider..."
az provider register --namespace 'Microsoft.AlertsManagement'
