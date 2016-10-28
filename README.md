## Running the stack

1. Configure the back-end:

  - Copy `/weyl-application/weyl.yml.example` to `/weyl-application/weyl.yml`.
  - Modify `applicationContextPath` as required.

2. Start the back-end:

        ./gradlew run -Pmemory=<MEMORY_SETTING>

3. Start the front-end:

        cd weyl-frontend
        npm start

4. Run data imports (can we found in the `dilectic` repo).
