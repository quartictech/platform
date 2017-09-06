#!/bin/sh
set -eu
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

psql -h localhost -a -p 15432 -U postgres -f ${DIR}/create-databases.sql
