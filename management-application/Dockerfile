FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD management-application-*.tar /
RUN ln -s /management-application-* /management-application

WORKDIR /management-application

CMD ./bin/management-application server management.yml
