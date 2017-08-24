## Prerequisites

- JDK 8 (plus unlimited strength JCE)

  ```
  brew cask install java
  brew cask install jce-unlimited-strength-policy
  ```

- NPM 4+

  ```
  brew install node
  ```
  
- Python 3 + Virtualenv

  ```
  brew install python3
  pip install virtualenv
  ```

- Ruby + Bundler

  ```
  brew install ruby
  gem install bundler
  ```

- aspell

  ```
  brew install aspell --with-lang-en
  ```


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
SKIP_FRONTEND= ./gradlew :catalogue:run :weyl:run --parallel
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

## Documentation

The stuff in `docs/` is based on [Zurb Foundation 6](http://foundation.zurb.com/sites/download.html/), with Panini
replaced with Jekyll to handle Markdown nicely.  Note that this means Jekyll is **not** managing asset files.

The whole thing is orchestrated by Gradle, as usual.  To run with a watch:

```
./gradlew docs:run

```

The spellchecking list is in `docs/wordlist`.  Please curate this carefully.


## Secrets

To avoid storing plaintext secrets in Git (either here or in the `infra` repo), all secrets that come in via
configuration should be encrypted as `EncryptedSecret`s (except for the master key).

Secrets can be encrypted via a fairly janky CLI, which can be run as follows:

```
./gradlew common-core:installDist
./common-core/build/install/common-core/bin/common-core [-g] [-d] [-f]
```

Flags:

- `-g` generates a random master key.
- `-d` decodes rather than encodes.
- `-f` encodes from a file.


### OAuth token signing key (`home`)

512 bits of entropy, base-64 encoded.  Can generate with something like:

```
println(SecureRandom().nextBytes(512 / 8).encodeAsBase64())
```


### GitHub client secret (`home`)

In our GitHub OAuth app settings, click **Reset client secret**.


### GitHub webhook secret (`glisten`)

Currently generated with:

```
pwgen -1s 20
```

Also needs to be stored in our GitHub (non-OAuth) app settings.


### GitHub private key (`qube`)

1. In our GitHub (non-Oauth) app settings, click **Regenerate private key**, and download.
2. Convert to a saner format we can load from Java
    ```
    openssl pkcs8 -topk8 -inform PEM -outform PEM -in private-key.pem -out private-key.der.pem -nocrypt
    ```
3. Crop the `BEGIN` and `END` lines so that just the key remains.


## Services

Service        | Port (app/admin) | Port (frontend dev)  | Description
---------------|------------------|----------------------|-----------------
Weyl           | 8080 / 8081      | 3000                 | Map UI
Catalogue      | 8090 / 8091      |                      | Dataset catalogue
Home           | 8100 / 8101      | 3010                 | Home UI
Howl           | 8120 / 8121      |                      | Cloud storage abstraction
~~Terminator~~ | ~~8130 / 8131~~  |                      |
Scribe         | 8140 / 8141      |                      | PubSub -> file storage batching
Rain           | 8150 / 8151      |                      | Howl-to-live-layer converter
Zeus           | 8160 / 8161      | 3020                 | Asset 360 UI
Glisten        | 8170 / 8171      |                      | GitHub webhook listener
~~Orf~~        | ~~8180 / 8181~~  |                      | ~~Authentication~~
Registry       | 8190 / 8191      |                      | Customer registry
Qube           | 8200 / 8201      |                      | Kubernetes abstraction
Hey            | 8220 / 8221      |                      | Slack notifications
