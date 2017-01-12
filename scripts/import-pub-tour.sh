#!/usr/bin/env bash
set -eu

curl -H Content-Type:application/json -XPOST http://localhost:8090/api/datasets -d '{
    "metadata": {
      "name": "Pub Tour",
      "description": "Arlo and Alex go wild",
      "attribution": "Quartic",
      "icon": "beer"
    },
    "map": {
        "viewType": "LOCATION_AND_TRACK",
        "titleAttribute": "name",
        "blessedAttributes": [ "name" ],
        "attributeTypes": {
          "timestamp": "TIMESTAMP"
        }
    },
    "locator": {
        "type": "websocket",
        "url": "ws://localhost:5000/"
    }
}'
