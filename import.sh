#!/bin/sh

CONTEXT_PATH=""
if [ "$#" -eq 1  ]; then
    CONTEXT_PATH=${1}
fi

API_ROOT=http://localhost:8080${CONTEXT_PATH}/api

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "UK Postcodes",
	"description": "All postcode centroids in the UK",
	"query": "SELECT * from uk_postcodes"
}'

 curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
 	"name": "Companies",
 	"description": "All LTD companies in the UK",
 	"query": "SELECT companyname,geom from companies_geocoded limit 1000000"
 }'

 curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
 	"name": "Postcode Districts",
 	"description": "Postcode districts in the UK",
 	"query": "SELECT * from postcode_districts"
 }'

 curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
 	"name": "London Boroughs",
 	"description": "London Borough Boundaries",
 	"query": "SELECT lb.name, lb.geom, lbp.* from london_borough_excluding_mhw lb left join london_borough_profiles lbp on lb.name = lbp.AreaName"
 }'

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "London House Sales",
	"description": "London House Sales and Prices",
	"query": "SELECT * from london_price_houses_geocoded"
}'

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "Public Land Assets",
	"description": "GLA Public Land Assets",
	"query": "SELECT * from public_land_assets_geocoded"
}'

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "McDonalds™",
	"description": "McDonalds™ Locations",
	"query": "SELECT * from mcdonalds_geocoded"
}'

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "London LSOAs",
	"description": "London Lower Super Output Areas",
	"query": "select * from lsoa_2011_london_gen_mhw"
}'


curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/roads.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/parking.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/nightlife.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/greenspace.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/buildings.json
#curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/tubes.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/rail.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/nightlife_query.json
curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d @json_imports/tube_query.json

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "Jamcams",
	"description": "TFL traffic camera feeds",
	"query": "select * from jamcams_geocoded"
}'

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "London Crime",
	"description": "London crime events for MET, BTP and City Police",
	"query": "select * from crime_geocoded where crimetype is not null limit 1000000"
}'

curl -XPUT -H Content-Type:application/json $API_ROOT/layer/import -d '{
	"name": "Green Belts 2014-2015",
	"description": "Local Authority Green Belt Boundaries 2014-2015",
	"query": "SELECT gb_name as name, area_ha, perim_km, la_name, year, ST_Force_2d(geom) as geom from local_authority_green_belt_boundaries_2014_15"
}'
