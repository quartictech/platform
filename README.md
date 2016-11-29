## Running the stack

1. Configure the back-end:

  - Copy `/weyl-application/weyl.yml.example` to `/weyl-application/weyl.yml`.
  - Modify `applicationContextPath` as required.

2. Start the back-end:

        ./gradlew run --parallel

3. Start the front-end:

        cd weyl-frontend
        npm start

4. Run data imports (can we found in the `dilectic` repo).

## Services
 - Weyl: 8080 / 8081
 - Catalogue: 8090 / 8091
 - Management: 8100 / 8101
 - Terminator: 8110 / 8111
 - Howl: 8120 / 8121


