## Running the stack locally

**Note:** The Scribe service writes to a real cloud bucket, even when running locally.

1. Start the back-end:

   ```
   SKIP_FRONTEND= ./gradlew run --parallel
   ```

2. Start the front-end:

    ```
    ./gradlew npmStart --parallel
    ```

3. Run data imports (using `weyl_imports` scripts in `dilectic` repo, or `scripts/import-pub-tour.sh`).

## Variations

To run a subset of services (example):

```
SKIP_FRONTEND= ./gradlew :catalogue-app:run :weyl-app:run --parallel
```

To run a service with reduced memory (example):

```
SKIP_FRONTEND= WEYL_MEMORY=4g ./gradlew run --parallel
```

## Building Docker images

```
./gradlew docker
```

Note that this will build the images with a registry name of `null` and a tag of `unknown`.  CircleCI overrides the
`QUARTIC_DOCKER_REPOSITORY` and `CIRCLE_BUILD_NUM` environment variables.


## Services

Service        | Port (app/admin) | Port (frontend dev)  | Notes
---------------|------------------|----------------------|-----------------
Weyl           | 8080 / 8081      | 3000                 |
Catalogue      | 8090 / 8091      |                      |
Mgmt           | 8100 / 8101      | 3010                 |
Howl           | 8120 / 8121      |                      |
~~Terminator~~ | ~~8130 / 8131~~  |                      |
Scribe         | 8140 / 8141      |                      |
Rain           | 8150 / 8151      |                      |
Zeus           | 8160 / 8161      | 3020                 | Hosted in separate repo
