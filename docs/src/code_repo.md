---
title: The code repository
subtitle: Structuring your code repository
---

Quartic requires only two things within your pipelines repository:
- a `quartic.yml` file exists at the top level.
- explicit pipeline steps are in a module folder (`__init__.py` exists.)

Both of these can be checked for and configured using the Quartic Command Line Interface (QLI).

## Setting up a repo

Your code is assumed to be in a Git repository and additionally hosted in a Github repository you have access to.

### Python environment

We strongly recommend you develop locally within a virtualenv. You should also be using Python3.

You should (but don't have to) install Quartic bindings and QLI by running `pip install quartic`. This will provide a more
pleasant local development experience and speed up validating your pipeline. Linting/IDE configuration 
is also easier in this case.

You're repository should be structured as follows (as far as Quartic related code):

```
root/
    quartic.yml #generated with `qli init` if needed
    <pipeline_dir_name>/
        __init__.py
        <filename1>.py
        <filename2>.py
        <pipeline_subdir>/
            <filename1>.py
            <filename2>.py
```

Note the following:
    - you can have as many levels of nesting as you like within any of your pipeline modules.
    - module directories are declared in `quartic.yml`. A default configuration can be produced using `qli init` which will your pipeline directory to `pipelines/`.

### External library dependencies

Any dependencies that your code relies on will be looked for within an optional `requirements.txt` at the root of the repository, compatible with `pip`.
You can produce this file in the usual way with `pip freeze > requirements.txt`.
