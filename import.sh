#!/bin/sh

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "Companies By LSOA",
	"description": "Companies grouped by LSOA (London)",
	"query": "SELECT * from companies_by_lsoa_london"
}'

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "Companies By Postcode",
	"description": "Companies grouped by Postcode",
	"query": "SELECT count, geom from postcode_district_company_count"
}'


curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "UK Postcodes",
	"description": "All postcode centroids in the UK",
	"query": "SELECT * from uk_postcodes"
}'

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "Companies",
	"description": "All LTD companies in the UK",
	"query": "SELECT companyname,geom from companies_geocoded limit 1000000"
}'

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "Postcode Districts",
	"description": "Postcode districts in the UK",
	"query": "SELECT * from postcode_districts"
}'
