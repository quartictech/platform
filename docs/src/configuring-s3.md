---
title: Configuring Amazon S3
---

Quartic can read and write data from a variety of sources, including S3 storage buckets associated with a customer AWS
account.  This note describes the configuration required to enable S3 integration.

## Integration overview

To maximise security, the Quartic platform accesses your S3 bucket without any long-term credentials being shared.
This is achieved as follows:

 - You create a dedicated S3 bucket under your AWS account.
 - You create a dedicated role under your AWS account.
 - You allow Quartic's AWS account to assume that role for operations associated with that particular bucket
   ("delegate access" in AWS terminology).

Before you begin the process below, Quartic will supply you with a unique "external ID" (needed for Step #2 below).
Once you're done, you'll need to provide Quartic with the following:

 - The __name__ and __region__ of your chosen S3 bucket (in Step #1 below).
 - The __ARN__ of the IAM role you create (in Step #2 below).

The Quartic engineering team will then test and confirm configuration.

## Configuration process
In these steps, we'll assume you're using the __AWS Management Console__, but all of this may be done programmatically
through the __AWS CLI__.

### 1. Create a dedicated S3 bucket in your AWS account

1. Go to __S3 → Create bucket__.
2. Fill in the required details.
   - The bucket name can be anything that fits your organisation's naming scheme (but we recommend something like
     `quartic-data.your-domain.com`).
   - The chosen region should take __Personally Identifiable Information (PII)__ regulations into account.
   - Feel free to enable or disable versioning, logging, and tags.

### 2. Create a new IAM role for Quartic

1. Go to __IAM → Roles → Create role → Another AWS account → Provide access between your AWS account and
   a 3rd party AWS account__, and fill in the following details:

   ```
   Account ID = 555071496850 (Quartic's AWS account ID)
   Require external ID = Ticked
   External ID = <External ID provided separately>
   Require MFA = Unticked
   ```

3. Click __Next: Permissions__, then __Next: Review__ (without attaching any policies).
4. Set the role name (can be anything, but we recommend something like `quartic-access`), then click
   __Create role__.
5. Once created, if you click on the newly-create role, the summary page will show a unique __Role ARN__ (of the form
   `arn:aws:iam::123456789012:role/xxxx`).

### 3. Create a policy allowing the IAM role full access to the bucket

1. Go to __IAM → Roles → `role_name` → Permissions → Add inline policy → Policy Generator → Select__ (where
   `role_name` is the name you chose in Step #2).
2. Fill in the following details:

   ```
   Effect = Allow
   AWS Service = Amazon S3
   Actions = All Actions
   Amazon Resource Name = arn:aws:s3:::bucket_name*
   ```

   where `bucket_name` is the name you chose in Step #1 (note the trailing *).

3. Click __Add Statement__, then click __Next step__, then __Apply Policy__.
