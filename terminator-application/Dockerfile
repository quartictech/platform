FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD terminator-application-*.tar /
RUN ln -s /terminator-application-* /terminator-application

WORKDIR /terminator-application

CMD ./bin/terminator-application server terminator.yml
