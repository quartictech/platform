#!/usr/bin/env bash
set -eu

curl -H Content-Type:application/json -XPUT http://localhost:8090/api/datasets -d '{
    "metadata": {
        "attribution": "Quartic",
        "description": "Pub Tour",
        "icon": "location arrow",
        "name": "Pub Tour"
    },
    "map": {
        "viewType": "LOCATION_AND_TRACK"
    },
    "locator": {
        "type": "websocket",
        "url": "ws://localhost:5000/"
    }
}'
