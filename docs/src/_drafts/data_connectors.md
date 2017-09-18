---
title: Data Connectors
subtitle: Getting data into the platform
---


Quartic is designed to accomodate data from different sources and of different
types.

Data can be registered with the platform in code, or via the web UI.


## Understanding raw datasets

Data may exist in two states at the start of your pipeline:

1) Static datasets:
   - there is an entry in the catalogue with a dataset ID.
   - there is a Quartic managed copy of the dataset in storage.
   - the dataset appears in the Datasets page of the web UI.
   - the dataset is addressable by a `step` using the dataset ID.

Files uploaded through the web UI always fall into this category.

2) Registered as a `raw` step. This means:
   - Quartic only knows of the location of the data outside the platform.
   - the data will be pulled in from the external location on every run of the pipeline.
   - there is a hidden copy of the dataset in the Datasets page of the web UI.
   Token cookie is missing - the dataset is only addressable by subsequent steps if the `raw` step is present.

This would be typically in the case where a user wishes to register an Amazon S3 bucket/directory/file
as a datasource. In this case, by default, the data is pulled from the source everytime the pipeline is run.

However, users may specify a `raw` step to be `static`. In this case, the dataset will be stored with a
permanent entry in the catalogue. This is typically useful if a user wishes to get a sample of dataset, or
if there's no guarantee that the dataset will always be there at runtime.

#### Raw steps

Raw steps are broadly like any other step in the pipeline and are declared using the decorator `@raw`.

<!-- If you wish to make the dataset static, you should pass `static=True` as an argument to the decorator: `@raw(static=True)`.

**WARNING:** You should view `static=True` as advanced functionality. -->

## Derived datasets

All steps produce datasets, these are referred to as `derived`.

## Data storage

Quartic is designed to leave storage of source data, and derived data to a bucket owned by you.

The platform can optionally be configured to provide storage for you if you wish.