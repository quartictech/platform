#!/bin/sh
set -eu
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

STACK=${1}
PAYLOAD_FILE=${DIR}/eval-trigger.${STACK}.json

curl -H Content-Type:application/json -XPOST http://localhost:8210/api/trigger -d @${PAYLOAD_FILE}
