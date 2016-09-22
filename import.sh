#!/bin/sh

#curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
#	"name": "UK Postcodes",
#	"description": "All postcode centroids in the UK",
#	"query": "SELECT * from uk_postcodes"
#}'

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

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "London Boroughs",
	"description": "London Borough Boundaries",
	"query": "SELECT * from london_borough_excluding_mhw lb left join london_borough_profiles lbp on lb.name = lbp.AreaName"
}'

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "London House Sales",
	"description": "London House Sales and Prices",
	"query": "SELECT * from london_price_houses_geocoded"
}'

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "Public Land Assets",
	"description": "GLA Public Land Assets",
	"query": "SELECT * from public_land_assets_geocoded"
}'

curl -XPUT -H Content-Type:application/json http://localhost:8080/api/layer/import -d '{
	"name": "McDonalds™",
	"description": "McDonalds™ Locations",
	"query": "SELECT * from mcdonalds_geocoded"
}'
