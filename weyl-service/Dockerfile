FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD build/distributions/weyl-service.tar /

WORKDIR /weyl-service
RUN mkdir cache

CMD ./bin/weyl-service server weyl.yml