FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD weyl-application-*.tar /
RUN ln -s /weyl-application-* /weyl-application

WORKDIR /weyl-application

CMD ./bin/weyl-application server weyl.yml
