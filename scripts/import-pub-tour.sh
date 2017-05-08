#!/usr/bin/env bash
set -eu

curl -H Content-Type:application/json -XPOST http://localhost:8090/api/datasets/production -d '{
    "metadata": {
      "name": "Pub Tour",
      "description": "Arlo and Alex go wild",
      "attribution": "Quartic"
    },
    "extensions": {
        "map": {
            "viewType": "LOCATION_AND_TRACK",
            "titleAttribute": "name",
            "blessedAttributes": [ "name" ],
            "attributeTypes": {
              "timestamp": "TIMESTAMP"
            }
        }
    },
    "locator": {
        "type": "websocket",
        "url": "ws://localhost:5000/"
    }
}'
