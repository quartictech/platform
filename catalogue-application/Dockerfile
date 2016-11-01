FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD catalogue-application-*.tar /
RUN ln -s /catalogue-application-* /catalogue-application

WORKDIR /catalogue-application

CMD ./bin/catalogue-application server catalogue.yml
