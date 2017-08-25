#!/usr/bin/env bash
set -eu

RESULTS=$(for f in build/bundle/*.html; do aspell list -l en_GB --add-filter=sgml --add-sgml-skip=code --add-extra-dicts=$(pwd)/wordlist < "${f}"; done)
echo ${RESULTS} | tr " " "\n" | awk '{print "TYPO >>>>> " $0}' | tee "${1}"
test "${RESULTS}" == ""
