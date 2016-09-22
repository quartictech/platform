## Running the stack

1. Configure and start the back-end:

  - Copy `/weyl-service/weyl.yml.example` to `/weyl-service/weyl.yml`.
  - Modify `applicationContextPath` as required.
  - Start the back-end:

        ./gradlew run -Pmemory=<MEMORY_SETTING>

2. Configure and start the front-end:

  - Copy `/weyl-frontend/weylConfig.js.example` to `/weyl-frontend/weylConfig.js`.
  - Modify `apiRoot` as required.
  - Start the front-end:

        cd weyl-frontend
        npm start

3. Run the data-import:

       ./import.sh [<CONTEXT_PATH>]
