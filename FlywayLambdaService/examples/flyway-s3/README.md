## General
A full working example of `flyway-lambda` deployment configured to execute migration scripts from S3 bucket  

## Deployment
Run the `cloudformation/deploy.sh` bash script to deploy all the AWS resources (VPC, DB, Lambda, S3 bucket, Secrets).
The script requires an existing S3 bucket to upload the CloudFormation stacks to.

## Migrations execution
Run `migrate.sh` to upload the sql migration files to the S3 bucket created by the `deploy.sh` script and execute them over the DB
The script outputs the response or errors to `response.json` and exists with either success (0) or error (1) 