#!/usr/bin/env bash
set -eu
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

PAYLOAD_FILE=${DIR}/github_push_event.json
SECRET_TOKEN="JaXAybVPJmDaLk2Z7fMx"

sig=$(openssl dgst -sha1 -hmac "${SECRET_TOKEN}" "${PAYLOAD_FILE}" | cut -f2 -d " ")

curl -XPOST http://localhost:8170/api/hooks/github \
  -H "Content-Type: application/json" \
  -H "X-Github-Event: push" \
  -H "X-Github-Delivery: 12345" \
  -H "X-Hub-Signature: sha1=${sig}" \
  --data-binary @"${PAYLOAD_FILE}"
