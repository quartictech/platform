FROM {{docker_base_image}}

ADD {{name}}-{{version}}.tar /
RUN \
    mv /{{name}}-{{version}} /service && \
    mv /service/{{name}}.yml /service/config.yml
WORKDIR /service
CMD ./bin/{{name}} server config.yml
