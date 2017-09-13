---
title: The Quartic Command Line Interface
---

The Quartic Python bindings provide a command line interface that can be used to enhance
your local development experience.

The following commands are available and invoked as `qli <command>`:

- `init`:
Set up the repository as a Quartic pipeline by generating a default `quartic.yml`.
This should be run at the root of your repository.

- `status`:
This can be thought of as similar to `git status`. Running this command show you how
many input and output nodes you have in your data pipeline.

- `validate`:
Check that your data pipeline conforms with the rules that mean it can be properly executed.
You may be prompted to run this after running `status` if problems are identified.

- `--help`:
Can be run by itself i.e. `qli --help` or on any of the above commands `qli validate --help`
for details about the command and usage.


