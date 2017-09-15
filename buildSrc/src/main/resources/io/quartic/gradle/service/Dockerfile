FROM {{docker_base_image}}

# See https://github.com/gliderlabs/docker-alpine/issues/279#issuecomment-300859312
RUN echo http://nl.alpinelinux.org/alpine/v3.6/main > /etc/apk/repositories; \
    echo http://nl.alpinelinux.org/alpine/v3.6/community >> /etc/apk/repositories

RUN \
    apk update && \
    apk add dumb-init=1.2.0-r0

ADD {{name}}-{{version}}.tar /
RUN \
    mv /{{name}}-{{version}} /service && \
    if [ -e /service/{{name}}.yml ]; then \
      mv /service/{{name}}.yml /service/config.yml; \
    fi
WORKDIR /service

ENTRYPOINT ["/usr/bin/dumb-init", "--"]         # To handle the PID 1 problem
CMD ["./bin/{{name}}", "server", "config.yml"]
