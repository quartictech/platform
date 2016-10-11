FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD weyl-service-*.tar /
RUN ln -s /weyl-service-* /weyl-service

WORKDIR /weyl-service

CMD ./bin/weyl-service server weyl.yml
