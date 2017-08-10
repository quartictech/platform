---
title: The domain specific language
subtitle: Getting to grips with the building blocks
---

QDSL is the Quartic platform specific language.
It provides a powerful and intuitive way to describe nodes and edges in a graph of operations on your data.
Strictly speaking the graph is a Directed Acyclic Graph or DAG.
This document is a basic introduction to the core concepts of the language and associated DAG concepts.

#### Building blocks of the DSL

QDSL is designed for use with Python3.

In addition, common data manipulation libraries are included in the associated library, and certain assumptions are made with respect to the users:
- Pandas dataframes will be used where possible.
- Spark or other cluster-computing frameworks are included, specific steps in the DAG may be submitted to a cluster.

We start with the following example of a node which is broken down into its core components:

```py
@step
def run(survey: "raw/survey") -> "clean/survey":
    survey_data = survey.data()

    for i in range(1, 4):
        input_col = "Measurement {}".format(i)
        output_col = "Measurement {} timestamp".format(i)
        survey_data[output_col] =
            survey_data[input_col].apply(datetime_to_unix)
        survey_data = survey_data.drop(input_col, axis=1)

    return writer("Cleaned survey").json({
        (d["Survey ID"]: d) for d in rows_as_dicts(survey_data)
    })
```


#### Defining steps

The `@step` decorator is used to define every node in the DAG:

```py
@step
```

This means that the function is a first-class step in the DAG. This means it will be executed
as part of a build sequence, with appropriate routing of inputs and outputs, and will appear as a node in the data pipeline.

#### Annotating I/O

```py
def run(survey: "raw/survey") -> "clean/survey"
    ...
    return
```

Input and output annotations specify the names of the datasets within the DAG. This is a feature of Python3 which is documented [here](https://www.python.org/dev/peps/pep-3107/).

These are optional - if present, Quartic uses them to
implicitly define nodes and edges in the DAG. If they're omitted, the user is expected to connect the nodes explicitly.

#### Accessing data

Input datasets contain data and associated [metadata]({{root}}/metadata). These can be accessed independently using the corresponding `.data()` and `.meta()` methods:

```py
survey_data = survey.data()
```

The result of a `.data()` call will return an object dependent on the underlying materialisation format. Where possible, the underlying file (e.g. Parquet) is
automatically convered to a Pandas dataframe for easier manipulation.

#### Manipulating data

```py
for i in range(1, 4):
        input_col = "Measurement {}".format(i)
        output_col = "Measurement {} timestamp".format(i)
        survey_data[output_col] =
            survey_data[input_col].apply(datetime_to_unix)
        survey_data = survey_data.drop(input_col, axis=1)
```

The above code block uses Pandas to manipulate the data. In this case, datetime columns are being replaced with Unix timestamps.

#### Producing datasets

```py
return writer("Cleaned survey").json({...})
```

Finally, an output dataset is produced. Here a human readable name is also specified and will form part of the metadata associated with the dataset.