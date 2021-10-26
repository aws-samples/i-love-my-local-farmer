#!/bin/bash

Help()
{
   echo "Syntax: migrate.sh --migration-bucket [s3 bucket with flyway migration scripts]"
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
    --migration-bucket)
      migration_bucket="$2"
      ;;
    *)
      printf "***************************\n"
      printf "* Error: Invalid argument.*\n"
      printf "***************************\n"
      exit 1
  esac
  shift
  shift
done

CheckArg $migration_bucket "--migration_bucket"

# Cleaning the response
trap '[ -e response.json ] && rm response.json' EXIT

echo "Uploading migration scripts to S3 bucket"
aws s3 cp ./sql/ s3://$migration_bucket/ --recursive

echo "Execute flyway migration"
resp=$(aws lambda invoke --cli-binary-format raw-in-base64-out --function-name FlywayLambdaS3 \
      --payload '{ "flywayRequest": {"flywayMethod": "migrate" }}' response.json | \
      jq --exit-status '.FunctionError == null' > /dev/null && echo 0;)
if [[ $resp -eq 0 ]]; then
  cat response.json;
  exit 0;
else
  [ -e response.json ] && cat response.json > /dev/stderr;
  exit 1;
fi
