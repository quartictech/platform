## Running the stack locally

**Note:** The Scribe service writes to a real cloud bucket, even when running locally.

1. Start the services (backend and frontend):

   ```
   SKIP_FRONTEND= ./gradlew run --parallel
   ```

2. Run data imports (using `weyl_imports` scripts in `dilectic` repo, or `scripts/import-pub-tour.sh`).

## Variations

To run a subset of services (example):

```
SKIP_FRONTEND= ./gradlew :catalogue-app:run :weyl-app:run --parallel
```

To run a service with reduced memory (example):

```
SKIP_FRONTEND= WEYL_MEMORY=4g ./gradlew run --parallel
```

## Frontend dependencies

Frontend dependencies aren't directly managed in `package.json`, because they're lame.  Instead, add `prod` and `dev`
entries to the `build.gradle` file for the relevant subproject.  `package.json` files are explicitly Git-ignored!

## Building Docker images

```
./gradlew docker
```

Note that this will build the images with a registry name of `null` and a tag of `unknown`.  CircleCI overrides the
`QUARTIC_DOCKER_REPOSITORY` and `CIRCLE_BUILD_NUM` environment variables.


## Services

Service        | Port (app/admin) | Port (frontend dev)  | Description
---------------|------------------|----------------------|-----------------
Weyl           | 8080 / 8081      | 3000                 | Map UI
Catalogue      | 8090 / 8091      |                      | Dataset catalogue
Mgmt           | 8100 / 8101      | 3010                 | Dataset management UI
Howl           | 8120 / 8121      |                      | Cloud storage abstraction
~~Terminator~~ | ~~8130 / 8131~~  |                      |
Scribe         | 8140 / 8141      |                      | PubSub -> file storage batching
Rain           | 8150 / 8151      |                      | Howl-to-live-layer converter
Zeus           | 8160 / 8161      | 3020                 | Asset 360 UI
Glisten        | 8170 / 8171      |                      | GitHub webhook listener
~~Orf~~        | ~~8180 / 8181~~  |                      | ~~Authentication~~
Registry       | 8190 / 8191      |                      | Customer registry
