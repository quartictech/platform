#!/bin/sh
curl -H Content-Type:application/json -XPOST http://localhost:8210/api/trigger -d @test.json
