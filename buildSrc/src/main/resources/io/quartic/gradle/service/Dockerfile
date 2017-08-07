FROM {{docker_base_image}}

ADD {{full_name}}-{{version}}.tar /
WORKDIR {{full_name}}-{{version}}
CMD ./bin/{{full_name}} server /{{full_name}}-{{version}}/{{simple_name}}.yml
