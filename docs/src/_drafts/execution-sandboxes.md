---
title: Execution sandboxes
subtitle: Quartic uses containers to provide security and reproducibility guarantees.
---

Pipeline steps are executed in shared-nothing sandbox containers.  Each sandbox communicates only via a dedicated API
gateway instance (see illustration below), and has no connectivity to the outside world.

This arrangement guarantees enforcement of namespace access controls, and prevents data leaks. Code reproducibility is
maximised by eliminating uncontrolled external dependencies and providing a known execution environment.

![Execution sandboxes](assets/img/execution-sandboxes.png){:width="50%" .center}