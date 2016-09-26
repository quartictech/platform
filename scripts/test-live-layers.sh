#!/bin/sh

API_ROOT=http://localhost:8080/api

ID=`curl -XPUT -H "Content-Type:application/json" "$API_ROOT/layer/live" -d '{"name":"Test Layer", "description": "TESTT"}'`

ID="${ID%\"}"
ID="${ID#\"}"

while true; do
	curl -XPOST -H "Content-Type:application/json" $API_ROOT/layer/live/$ID -d@scripts/test.json.0
	sleep 2
	curl -XPOST -H "Content-Type:application/json" $API_ROOT/layer/live/$ID -d@scripts/test.json.1
	sleep 2
	curl -XPOST -H "Content-Type:application/json" $API_ROOT/layer/live/$ID -d@scripts/test.json.2
	sleep 2
	curl -XPOST -H "Content-Type:application/json" $API_ROOT/layer/live/$ID -d@scripts/test.json.3
	sleep 2
done
