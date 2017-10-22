FROM openjdk:8u131-jre-alpine

# See https://github.com/gliderlabs/docker-alpine/issues/279#issuecomment-300859312
RUN echo http://nl.alpinelinux.org/alpine/v3.6/main > /etc/apk/repositories; \
    echo http://nl.alpinelinux.org/alpine/v3.6/community >> /etc/apk/repositories

RUN \
    apk update && \
    apk add dumb-init=1.2.0-r0

ADD service.tar /
WORKDIR /service

ENTRYPOINT ["/usr/bin/dumb-init", "--"]         # To handle the PID 1 problem
CMD ["./bin/service", "server", "config.yml"]
