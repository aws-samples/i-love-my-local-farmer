#!/bin/bash

Help()
{
   echo "Syntax: deploy.sh --stack [stack name] --bucket [cloudformation s3 bucket] --db-name [database name] --db-user [database username] --migration-bucket [s3 bucket with flyway migration scripts]"
   echo
}

CheckArg()
{
  if [ ! $# -eq 2 ]; then
    echo "Required $1 option not provided"
    Help
    exit 1
  fi
}

while [ $# -gt 0 ]; do
  case "$1" in
    --stack)
      stack_name="$2"
      ;;
    --bucket)
      bucket="$2"
      ;;
    --db-name)
      db_name="$2"
      ;;
    --db-user)
      db_user="$2"
      ;;
    --migration-bucket)
      migration_bucket="$2"
      ;;
    --h)
      Help
      exit;;
    *)
      printf "***************************\n"
      printf "* Error: Invalid argument.*\n"
      printf "***************************\n"
      exit 1
  esac
  shift
  shift
done

CheckArg $stack_name "--stack"
CheckArg $bucket "--bucket"
CheckArg $db_name "--db-name"
CheckArg $db_user "--db-user"
CheckArg $migration_bucket "--migration-bucket"

echo "Downloading latest flyway-lambda"
if [ ! -e  flyway-all.jar ]; then
  wget https://github.com/Geekoosh/flyway-lambda/releases/latest/download/flyway-all.jar
fi

echo "Packaging CloudFormation templates towards deploy"
aws cloudformation package --template-file ./stack.yaml --output-template-file ./stack-out.yaml --s3-bucket $bucket

echo "Deploying CloudFormation templates"
aws cloudformation deploy --template-file ./stack-out.yaml --stack-name $stack_name \
  --parameter-overrides DBName=$db_name Username=$db_user S3Bucket=$migration_bucket \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
