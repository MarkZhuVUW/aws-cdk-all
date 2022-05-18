#!/usr/bin/env bash
sed -i 's/\r$//'

set -e
clear

cd ..
yes | cdk destroy -vv --force IAM-stuff