FROM openjdk:8u92-jre-alpine
ARG full_name
ARG simple_name
ARG version

# TODO: let's find a way not to need this - something about the startup script is Bash-specific
RUN apk --no-cache add bash

ADD @full_name@-@version@.tar /
WORKDIR @full_name@-@version@
CMD ./bin/@full_name@ server /@full_name@-@version@/@simple_name@.yml
