---
title: Execution sandboxes
subtitle: Quartic uses containers to provide security and reproducibility guarantees.
---

Pipeline steps are executed in shared-nothing sandbox containers.  Each sandbox communicates only via a dedicated API
gateway instance (see illustration below), and has no connectivity to the outside world.

This arrangement guarantees enforcement of namespace access controls, and prevents data leaks. Code reproducibility is
maximised by eliminating uncontrolled external dependencies and providing a known execution environment.

![Execution sandboxes](assets/img/execution-sandboxes.png){:width="50%"}

## The DSL and DAG

Quartic offers an intuitive Domain Specific Language (DSL) for building pipeline Directed Acyclic Graphs (DAGs). A Python example is shown
below, though bindings are available for other languages, and new ones are easy to build using the platform APIs.

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

```py
@steps
```

The `@step` decorator denotes that this function is a first-class step in the DAG. This means it will be executed
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

Input datasets contain data and associated metadata. These can be accessed independently using the corresponding `.data()` and `.meta()` methods:

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

