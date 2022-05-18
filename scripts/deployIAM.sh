#!/usr/bin/env bash
sed -i 's/\r$//'

set -e
clear

cd ..
cdk deploy -vv --require-approval never IAM-stuff