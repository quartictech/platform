# AWS CLI for Howl S3 test

## Create bucket

aws s3api create-bucket --bucket test-howl --region eu-west-1 --create-bucket-configuration LocationConstraint=eu-west-1

## Create bucket-access role

aws iam create-role --role-name Test-Bucket-Accessor --assume-role-policy-document file://$(pwd)/trust-policy.json
aws iam put-role-policy --role-name Test-Bucket-Accessor --policy-name AccessBucket --policy-document file://$(pwd)/access-bucket-policy.json

## Create tester group

aws iam create-group --group-name Platform-Testers
aws iam put-group-policy --group-name Platform-Testers --policy-name AssumeRole --policy-document file://$(pwd)/assume-role-policy.json

**Note:** The role ARN is hardcoded in `assume-role-policy.json`.

**Note:** You'll need to add all users to this group.