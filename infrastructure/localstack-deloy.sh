#!/bin/bash

set -e  # Exit if any command fails
aws --endpoint-url=http://localhost:4566 cloudformation delete-stack --stack-name patient-management
ENDPOINT="http://localhost:4566"
STACK_NAME="patient-management"
TEMPLATE_FILE="./cdk.out/localstack.template.json"

echo "🚀 Deploying CloudFormation stack to LocalStack..."

aws --endpoint-url=$ENDPOINT cloudformation deploy \
    --stack-name $STACK_NAME \
    --template-file "$TEMPLATE_FILE"

echo "📡 Fetching Load Balancer DNS name..."

aws --endpoint-url=$ENDPOINT elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName"  --output text
