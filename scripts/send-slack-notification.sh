#!/usr/bin/env bash
set -eu
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


curl -XPOST http://localhost:8220/ \
  -H "Content-Type: application/json" \
  -d @"${DIR}/slack-notification.json"
