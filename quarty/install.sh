#!/usr/bin/env bash
#--------------------------------------------------------#
# !!! This script is intended to be called by Gradle !!!
#--------------------------------------------------------#

VIRTUALENV_DIR=${1}
source ${VIRTUALENV_DIR}/bin/activate

set -eu

pip install pip-tools

pip-compile

# Workaround for https://github.com/jazzband/pip-tools/issues/204
# (adapted from https://github.com/jazzband/pip-tools/issues/331#issuecomment-285825098)
sed "s|-e file://$(pwd)|-e .|" < requirements.txt > requirements.workaround.txt

# Workaround for https://github.com/jazzband/pip-tools/pull/555 (due in the next pip-tools release)
if [ -e /etc/debian_version ]; then
    echo "pkg-resources==0.0.0" >> requirements.workaround.txt
fi

pip-sync requirements.workaround.txt