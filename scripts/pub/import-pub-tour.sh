#!/usr/bin/env bash
set -eu

curl -H Content-Type:application/json -XPUT http://localhost:8090/api/datasets/noobhole/abc%2fdef -d '{
    "metadata": {
      "name": "Pub Tour",
      "description": "Arlo and Alex go wild",
      "attribution": "Quartic"
    },
    "locator": {
        "type": "websocket",
        "url": "ws://localhost:5000/"
    }
}'
