#!/usr/bin/env bash
#--------------------------------------------------------#
# !!! This script is intended to be called by Gradle !!!
#--------------------------------------------------------#

VIRTUALENV_DIR=${1}
source ${VIRTUALENV_DIR}/bin/activate

set -eu

# 1.10.0 currently seems to break with "TypeError: unorderable types: InstallRequirement() < InstallRequirement()"
pip install pip-tools==1.9.0

export CUSTOM_COMPILE_COMMAND="./gradlew createVirtualenvAndInstallDeps"
pip-compile

# Workaround for https://github.com/jazzband/pip-tools/issues/204
# (adapted from https://github.com/jazzband/pip-tools/issues/331#issuecomment-285825098)
temp_file=$(mktemp)
sed "s|-e file://$(pwd)|-e .|" < requirements.txt > ${temp_file}
mv ${temp_file} requirements.txt

# Workaround for https://github.com/jazzband/pip-tools/pull/555 (due in the next pip-tools release)
if [ -e /etc/debian_version ]; then
    cp requirements.txt requirements.workaround.txt
    echo "pkg-resources==0.0.0" >> requirements.workaround.txt
    pip-sync requirements.workaround.txt
else
    pip-sync requirements.txt
fi