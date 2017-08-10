---
title: Metadata
subtitle: Fine-grained control of the DAG
---

By default, Quartic associates metadata to every node in the DAG.
This can be left completely untouched - the default behaviour and content will be suitable for most uses, certainly in simple cases. These defaults are also configurable either via the frontend, or via the config file.

For finer grained control of the DAG, specifically orchestration, monitoring, and validation, read on.

Advanced features regarding access controls, geographic region control, user assignments and so on are left to the end.


#### Accessing metadata on a node

Metadata on a node and dataset is accessed via the associated method `.meta()` which will return a Python dictionary-like object.
Values within the metadata object may be accessed as expected, i.e.:

```py
meta = data.meta()
name = meta['ds_name']
```

It is important at this point to understand the distinction between the node, the dataset, metadata, and configuration.

The DAG is comprised of nodes containing datasets and described by metadata.
This means the metadata is distinct from the data, although some of the entries (read-only) are produced when the node computation is evaluated.

##### Default key-value pairs on metadata

A number of default key/value pairs are set on the metadata object.
Additionally, some of these are read-only. 

The default list and corresponding keys and return types are as follows:
- node ID (read-only) ('id' -> String)
- dataset ID (read-only) ('id' -> String)
- dataset creation time (read-only) (`ts` -> Datetime)
- git commit associated with the last creation (read-only) (`commit` -> String)
- parent nodes (read-only) (`parents` -> List<String>)
- child nodes (read-only) (`child` -> List<String>)
- monitoring configuration ??
- validation configuration ??
- orchestration configuration ??
- node name (`node_name` -> String)
- dataset name (`ds_name` -> String)
- dataset description (`ds_desc` -> String)
- dataset attribution (`ds_attr` -> String)

Non-default entries may be set by, and accessed by the user as needed to pass around variables and values that are not part of the actual data.

#### Setting node configuration via metadata

Fine grained control over the DAG can be achieved by specifying a configuration for some or all of the individual nodes in the DAG.

This is achieved by passing a dictionary to the `@step` decorator used to declare a node.

```py
my_config = {node_name : 'example node', ds_name : 'example dataset'}
@step(my_config)
def run ...
```
