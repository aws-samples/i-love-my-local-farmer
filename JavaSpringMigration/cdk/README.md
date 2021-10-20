# ECS and ECR Infrastructure for Provman App

This directory contains  CDK code for deploying the Provman application into the cloud.

## Deployment

This CDK code can be deployed in just a few steps,

- First, modify the values in `cdk.context.json` to contain the values for your accounts and regions

- Second, run `cdk deploy --all` to deploy the resources

This will setup a CI/CD pipeline that will then deploy the necessary infrastructure for an Application Load Balanced
Fargate ECS service.

In the event that you wish for the pipeline to track your own repo, the repository used for the Codepipeline deployments can be set in the
`cdk/src/main/java/com/ilmlf/product/cicd/PipelineStack.java` file