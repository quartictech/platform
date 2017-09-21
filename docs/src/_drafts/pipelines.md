---
title: Data pipelines
---

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

The DAG is composed of data transformations as above.
Each transformation takes an arbitrary number of input datasets and produces a single output dataset.

The DAG structure provides several key features of the platform:
- code may be versioned and divided sensibly between the nodes
- computations and sections of the DAG are only reevaluated when input data and/or specific code has changed (see diagram below)
- Data scientists may hook models into any intermediate datasets and may share previous work down - exactly analogous to branching/forking within a VCS
