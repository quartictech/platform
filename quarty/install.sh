#!/usr/bin/env bash

VIRTUALENV_DIR=${1}
source ${VIRTUALENV_DIR}/bin/activate

set -eu

pip install pip-tools

pip-compile

# Workaround for https://github.com/jazzband/pip-tools/issues/204
# (adapted from https://github.com/jazzband/pip-tools/issues/331#issuecomment-285825098)
sed -i '' "s|-e file://$(pwd)|-e .|" requirements.txt

# Workaround for https://github.com/jazzband/pip-tools/pull/555 (due in the next pip-tools release)
if [ -e /etc/debian_version ]; then
    cp requirements.txt requirements.debian-workaround.txt
    echo "pkg-resources==0.0.0" >> requirements.debian-workaround.txt
    pip-sync requirements.debian-workaround.txt
else
    pip-sync
fi