#!/bin/bash

echo -e "==> Setting variables..."
PROJECT=''
RESOURCE_GROUP=''
LOCATION=''

#echo -e "==> Creating resource group..."
#az group create \
#    --name $RESOURCE_GROUP \
#    --location $LOCATION

echo -e "==> Deploying template..."
az deployment group create \
    --name 'Microsoft.Deployment' \
    --resource-group $RESOURCE_GROUP \
    --template-file ./src/infra/main.json
