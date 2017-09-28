#!/usr/bin/env bash
#--------------------------------------------------------#
# !!! This script is intended to be called by Gradle !!!
#--------------------------------------------------------#

VIRTUALENV_DIR=${1}
source ${VIRTUALENV_DIR}/bin/activate

set -eu

pip install pip-tools==1.10.1

export CUSTOM_COMPILE_COMMAND="./gradlew createVirtualenvAndInstallDeps"
pip-compile

# Workaround for https://github.com/jazzband/pip-tools/issues/204
# (adapted from https://github.com/jazzband/pip-tools/issues/331#issuecomment-285825098)
temp_file=$(mktemp)
sed "s|-e file://$(pwd)|-e .|" < requirements.txt > ${temp_file}
mv ${temp_file} requirements.txt
