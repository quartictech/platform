FROM openjdk:8u92-jre-alpine

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD howl-application-*.tar /
RUN ln -s /howl-application-* /howl-application

WORKDIR /howl-application

CMD ./bin/howl-application server howl.yml
