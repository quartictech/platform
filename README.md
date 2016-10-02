## Running the stack

1. Configure the back-end:

  - Copy `/weyl-service/weyl.yml.example` to `/weyl-service/weyl.yml`.
  - Modify `applicationContextPath` as required.

2. Start the back-end:

        ./gradlew run -Pmemory=<MEMORY_SETTING>

3. Start the front-end:

        cd weyl-frontend
        npm start

4. Run the data-import:

        ./import.sh [<CONTEXT_PATH>]
