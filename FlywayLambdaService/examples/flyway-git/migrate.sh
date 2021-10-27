#!/bin/bash

Help()
{
   echo "Syntax: migrate.sh --branch [git branch name]"
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
    --branch)
      branch="$2"
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

CheckArg $branch "--branch"

# Cleaning the response
trap '[ -e response.json ] && rm response.json' EXIT

echo "Execute flyway migration"
resp=$(aws lambda invoke --cli-binary-format raw-in-base64-out --function-name FlywayLambdaGit \
      --payload "{ \"flywayRequest\": {\"flywayMethod\": \"migrate\" }, \"gitRequest\": {\"gitBranch\": \"$branch\"}}" response.json | \
      jq --exit-status '.FunctionError == null' > /dev/null && echo 0;)
if [[ $resp -eq 0 ]]; then
  cat response.json;
  exit 0;
else
  [ -e response.json ] && cat response.json > /dev/stderr;
  exit 1;
fi
