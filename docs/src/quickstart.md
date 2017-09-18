---
title: Quickstart
subtitle: Building your first pipeline
---

The following is a shortcut guide to start building data pipelines within Quartic.
This is intended to skim over features and functionality of the platform and focus only on getting you up and running with an example.

The following assumes you have already created an account. If not, [email us](mailto:contact@quartic.io).

### Creating a data pipeline repository

Data pipelines are entirely described in Python.

An example data pipeline is [available on GitHub](https://github.com/quartictech/hello-quartic). This code derives several datasets from the example data source.

If you'd like to run this code, or use it as a starting point for your own pipeline, please fork the repository to your own Github account and connect it to Quartic.

### Connecting a data source

You can connect data sources from a number of providers, as well as upload raw data that you may have locally.

To simply get started, we recommend doing one of the following:

- upload some data from the frontend. This data will then be available via the data URL displayed on the dataset for use in your pipeline.

- (more advanced) define a raw dataset.

If you'd rather leave using your own data to a later stage, an example data source is provided when you first start the instance.


### Adding a node to the data pipeline

The following defines a complete step in the pipeline:

```py
@step
def run(input_dataset: "input_dataset_id") -> "output_dataset_id":
    input = input_dataset.reader().raw().read()

    #some data manipulation here
    output = ...

    return writer("output dataset name", "output dataset description").json(output)
```

Working through this piece by piece:

```py
@step
```

defines a new step in the pipeline. Followed by:

```py
def run(input_dataset: "input_dataset_id") -> "output_dataset_id"
    #manipulate the input dataset here. For example remove a column
    ...
    #then return the dataset which will be written to output_dataset_name
    return ...
```

to manipulate the input dataset(s) and produce an output dataset.
Note, this does not yet run the pipeline nor write anything anywhere.

Multiple datasets may be manipulated by adding these as arguments to your function:

```py
def run(input1: "the_first_input_id", input2: "the_second_input_id") ...
```

Finally, you should return a `writer` object which you will need to import from the `quartic` library.
Learn more about `writer` in the DSL section.

### Running the pipeline

A Quartic pipeline has two concepts related to running - validating, and executing.

Validation is run every time you commit your code to your GitHub repository. Additionally,
limited local validation may be run with `qli validate`.

Executing the pipeline produces datasets at each step and is triggered from the web UI.

### Next steps

Congrats! You've just built and run your first data pipeline with monitoring, automation,
version control, and provenance.

Next, we suggest you move on to the slightly longer guide to learn about the DAG, and revisit any topics
highlighted above to understand more about the platform.