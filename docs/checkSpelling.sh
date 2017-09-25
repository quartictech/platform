#!/usr/bin/env bash
#--------------------------------------------------------#
# !!! This script is intended to be called by Gradle !!!
#--------------------------------------------------------#

set -eu

RESULTS=$(for f in build/bundle/*.html; do cat ${f} | sed s/[”“]/'"'/g | sed s/[’]/\'/g | aspell list -l en_GB --add-filter=sgml --add-sgml-skip=code --add-extra-dicts=$(pwd)/wordlist; done)
echo ${RESULTS} | tr " " "\n" | awk '{print "TYPO >>>>> " $0}' | tee "${1}"
test "${RESULTS}" == ""
