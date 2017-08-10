---
title: Quickstart
subtitle: Building your first pipeline
---

The following is a shortcut recipe to start building data pipelines within Quartic.
This is intended to skim over features and functionality of the platform and focus only on getting you up and running with an example.

The following assume you have already created an account. If not, you can do that here.

### Connecting a data bucket

You can connect data sources from a number of providers, as well as upload raw data that you may have locally.

An example data source is provided when you first start the instance.

### Creating an example data pipeline

Example data pipeline code may be found here. This code derives several datasets from the example data source.

If you'd like to run this code as a pipeline, feel free to clone or fork the repository to your own Github account and connect it to Quartic.

This will provide a complete, basic, end-to-end example of basic features.

### Adding a node to the data pipeline

The following Python syntax is used to add a node to the pipeline.

```py
@step
```

defines a new step in the pipeline. Followed by:

```py
def run(input_dataset: "input_dataset_name") -> "output_dataset_name"
    #manipulate the input dataset here. For example remove a column
    ...
    #then return the dataset which will be written to output_dataset_name
    return ...
```

to manipulate the input dataset(s) and produce an output dataset.
Note, this does not yet run the pipeline nor write anything anywhere.

### Running the pipeline

Hit the play button somewhere.