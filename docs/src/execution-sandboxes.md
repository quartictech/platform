---
title: Execution sandboxes
subtitle: Quartic uses containers to provide security and reproducibility guarantees.
---

Pipeline steps are executed in shared-nothing sandbox containers.  Each sandbox communicates only via a dedicated API
gateway instance (see illustration below), and has no connectivity to the outside world.

This arrangement guarantees enforcement of namespace access controls, and prevents data leaks.  Code reproducibility is
maximised by eliminating uncontrolled external dependencies and providing a known execution environment.

![Execution sandboxes](assets/img/execution-sandboxes.png){:width="50%"}

## Some more stuff.

Quartic offers an intuitive domain specific language (DSL) for building pipeline DAGs.   A Python example is shown
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