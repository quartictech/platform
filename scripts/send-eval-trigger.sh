#!/bin/sh
set -eu
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

PAYLOAD_FILE=${DIR}/eval_trigger.json.prod

curl -H Content-Type:application/json -XPOST http://localhost:8210/api/trigger -d @$PAYLOAD_FILE
