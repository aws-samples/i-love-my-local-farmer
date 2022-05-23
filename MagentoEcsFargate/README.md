# Containerizing our Magento stack

This directory contains only architecture and installation steps. For more details, please check the [blog] (https://medium.com/i-love-my-local-farmer-engineering-blog/xxxx) 

## Prerequisites
1. **NodeJS (v. 12+)** NodeJS is required for running and installing AWS CDK. You can download it [here](https://nodejs.org/en/download/).
1. **AWS CDK (v. 2.24.1)**: This solution uses AWS CDK for deployment. If you aren't familiar with CDK, please install its [prerequisites](https://cdkworkshop.com/15-prerequisites.html) and follow the  [Python workshop](https://cdkworkshop.com/30-python.html) first.   

## Architecture
To understand what this solution will deploy, we will start from the original architecture with virtual machine deployed on prem

![](.README_images/on-prem-architecture.png)

The solution uses Amazon ECS on Fargate to host the Magento App, Amazon RDS for MariaDB and Amazon Opensearch as dependencies.

![](.README_images/on-aws.jpg)


### !!! This example is provided as a sample and rely on HTTP, which should never be used in production system . Use [encrypted traffic] (https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html) instead .!!! 

## Components 

The main components of these solutions are in the folder magento_platform :

1. `magento_platform_stack` will set up AWS VPC and Security groups
2. `magento_db_stack` will set up the Amazon RDS for MariaDB.
3. `magento_es_stack` will set up the Opensearch cluster.
4. `magento_app_stack` will set up Amazon ECS cluster on Fargate to host the Magento Application.  


## Installation steps

1. cd MagentoEcsFargate (if not already)
2. python3 -m virtualenv venv
3. source .venv/bin/activate
4. pip3 install -r requirements.txt
5. cdk deploy --all
6. In CloudFormation console, get the output "MagentoHostname" from the MagentoAppStack and connect to http://#MagentoHostname#/admin with magento_user. For the password retreive the value of "passwordMagentoUser" in Amazon Secret Manager in the AWS Console 

## Need to know 

We have 2 environement variables commented in the magento_app_stack. They need to remain commented during the first initialization of the platform :

`MAGENTO_SKIP_BOOTSTRAP` : Whether to skip performing the initial bootstrapping for the application. Default: no
`MAGENTO_SKIP_REINDEX` : Whether to skip Magento re-index during the initialization. Default: no
