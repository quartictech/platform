FROM {{docker_base_image}}

ADD {{name}}-{{version}}.tar /
RUN \
    mv /{{name}}-{{version}} /service && \
    if [ -e /service/{{name}}.yml ]; then \
      mv /service/{{name}}.yml /service/config.yml; \
    fi
WORKDIR /service
CMD ./bin/{{name}} server config.yml
