FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD jester-application-*.tar /
RUN ln -s /jester-application-* /jester-application

WORKDIR /jester-application

CMD ./bin/jester-application server jester.yml
