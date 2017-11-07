## Prerequisites

- JDK 8 (plus unlimited strength JCE)

  ```
  brew cask install java
  brew cask install jce-unlimited-strength-policy
  ```

- Yarn 1.0.2+

  ```
  brew install yarn
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

- Docker

  ```
  brew install docker
  ```


## Running the stack locally

In general, you won't want or need to run the entire stack locally, because it will be slow and may require too much
memory.  So a subset of services can be run as in the following example:

```
./gradlew home:run eval:run registry:run postgres:run --parallel
```

Frontend components are best run separately in development (to benefit from hot reloading).  So in another terminal:

```
./gradlew home-front:run
```


## Local dev

Get a local DB dump by doing the following:

```
kubectl port-forward -n platform `./get-pod platform postgres` 5432
```

(from the infra repo.)

Then:

```
pg_dumpall -h localhost -p 5432 -U postgres > dump.sql
```

Then:

```
psql -h localhost -p 15432 -U postgres eval < dump.sql
```

You'll need postgres running before the last step. You may also need to do a number of drop/create before this will work
if you have the DB already created from a previous run.

Finally, make sure the number in `registry/registry.yml` matches the customer number in the build table. Get this by
running `select * from build;` when in the `eval` DB.


## Setting up Postgres

If you're running a service that requires Postgres, you'll need to first create the databases.  Once the Postgres
service is running, invoke the following:

```
./scripts/db/create-databases.sh
```


## Frontend dependencies

Frontend dependencies aren't directly managed in `package.json`, because they're lame.  Instead, add `prod` and `dev`
entries to the `build.gradle` file for the relevant subproject.  `package.json` files are explicitly Git-ignored!


## Python dependencies

Python dependencies are managed via [pip-tools][1], in order to ensure that the local virtualenv contains *only* the
current dependencies (as opposed to retaining old dependencies).  The workflow is entirely managed by Gradle, which
delegates some of the complexity to a wrapper script (`install.sh`).

Requirements should be listed in the relevant section of `setup.py`.  `pip-tools` then treats `requirements.txt` as a
lock-file, so you shouldn't edit this directly. 


[1]: https://github.com/jazzband/pip-tools


## Building Docker images

```
./gradlew docker
```

Note that this will build the images with a registry name of `null` and a tag of `0`.  CircleCI overrides the
`QUARTIC_DOCKER_REPOSITORY` and `CIRCLE_BUILD_NUM` environment variables.


## Documentation

The stuff in `docs/` is based on [Zurb Foundation 6](http://foundation.zurb.com/sites/download.html/), with Panini
replaced with Jekyll to handle Markdown nicely.  Note that this means Jekyll is **not** managing asset files.

The whole thing is orchestrated by Gradle, as usual.  To run with a watch:

```
./gradlew docs:run

```

The spellchecking list is in `docs/wordlist`.  Please curate this carefully.


## GitHub integration configuration

GitHub integrations come in [two flavours][1] - GitHub apps and OAuth apps.  We need both:

- we use the GitHub app for webhooks and for making build-related API queries (e.g. to get short-lived access tokens).
- we use the OAuth app for frontend user auth.  

In the following instructions, `${STACK_NAME}` will be something like `Staging`, and `${DOMAIN}` will be something
like `api.staging.quartic.io`.

[1]: https://developer.github.com/apps/building-integrations/setting-up-a-new-integration/about-choosing-an-integration-type/


### GitHub app

1. Start [here](https://github.com/organizations/quartictech/settings/apps), and click
   **Register a new application**.

2. Fill in the details:

    - GitHub App name: **Quartic (`${STACK_NAME}`)**
    - Homepage URL: **https://www.quartic.io**
    - Webhook URL: **https://`${DOMAIN}`/api/hooks/github**
    - Webhook secret: [See here](#github-webhook-secret)

3. Enable the following permissions:

    - Commit statuses: **Read & write**
    - Repository contents: **Read-only** (and tick **Push**)

4. Tick **Any account**.

5. Click **Create GitHub App**.

6. Click **Generate private key** ([see here](#github-private-key)).


### OAuth app

1. Start [here](https://github.com/organizations/quartictech/settings/applications), and click
   **Register a new application**.

2. Fill in the details:

    - Application name: **Quartic (`${STACK_NAME}`)**
    - Homepage URL: **https://www.quartic.io**
    - Authorization callback URL: **https://`${DOMAIN}`/api/auth/gh/callback**
  
3. Click **Register application**.


## Integration tests

There are various tests covering integration with 3rd-party services, which require some external configuration.


### GitHub

See `GitHubShould`.  Requirements:

- A `Quartic (Dev)` GitHub app

  - Configured [the normal way](#github-app).
  - Installed on the `quartictech` repo.
  - A private key, encoded [the normal way](#github-private-key).
  
- A [`quartic-platform-test`](https://github.com/quartic-platform-test) service account (creds in the usual place).

  - Registed a "personal access token".  Start [here](https://github.com/settings/tokens), click **Generate new token**,
    and ensure it has `read:org` and `read:user` permissions.
  - Invited and added to the [`noobhole`](https://github.com/orgs/noobhole/people) organisation.


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


### OAuth token signing key
 
**(Needed by `home`)**

512 bits of entropy, base-64 encoded.  Can generate with something like:

```
println(SecureRandom().nextBytes(512 / 8).encodeAsBase64())
```


### GitHub client secret

**(Needed by `home`)**

In our GitHub OAuth app settings, click **Reset client secret**.


### GitHub webhook secret

**(Needed by `glisten`)**

Currently generated with:

```
pwgen -1s 20
```

Also needs to be stored in our GitHub (non-OAuth) app settings.


### GitHub private key

**(Needed by `eval`)**

1. In our GitHub (non-Oauth) app settings, click **Regenerate private key**, and download.
2. Convert to a saner format we can load from Java
    ```
    openssl pkcs8 -topk8 -inform PEM -outform PEM -in private-key.pem -out private-key.der.pem -nocrypt
    ```
3. Crop the `BEGIN` and `END` lines so that just the key remains.


## Services

Service        | Port (backend) | Port (frontend dev)  | Description
---------------|----------------|----------------------|-----------------
~~Weyl~~       | ~~8080~~       | ~~3000~~             | ~~Map UI~~
Catalogue      | 8090           |                      | Dataset catalogue
Home           | 8100           | 3010                 | Home UI
Howl           | 8120           |                      | Cloud storage abstraction
~~Terminator~~ | ~~8130~~       |                      |
~~Scribe~~     | ~~8140~~       |                      | ~~PubSub -> file storage batching~~
~~Rain~~       | ~~8150~~       |                      | ~~Howl-to-live-layer converter~~
Zeus           | 8160           | 3020                 | Asset 360 UI
Glisten        | 8170           |                      | GitHub webhook listener
~~Orf~~        | ~~8180~~       |                      | ~~Authentication~~
Registry       | 8190           |                      | Customer registry
Qube           | 8200           |                      | Kubernetes abstraction
Eval           | 8210           |                      | Deals with graph evaluation
Hey            | 8220           |                      | Slack notifications

## License

This project is made available under [BSD License 2.0](https://github.com/quartictech/platform/blob/develop/LICENSE).
