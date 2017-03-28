#!/usr/bin/env python3
import random
import json
from datetime import timedelta, datetime, date
from shapely.geometry import shape, Point

def default(obj):
    """Default JSON serializer."""
    import calendar, datetime

    if isinstance(obj, datetime.datetime):
        if obj.utcoffset() is not None:
            obj = obj - obj.utcoffset()
        millis = int(
            calendar.timegm(obj.timetuple()) * 1000 +
            obj.microsecond / 1000
        )
        return millis
    raise TypeError('Not sure how to serialize %s' % (obj,))

def digit_to_char(digit):
    if digit < 10:
        return str(digit)
    return chr(ord('a') + digit - 10)

def str_base(number,base):
    if number < 0:
        return '-' + str_base(-number, base)
    (d, m) = divmod(number, base)
    if d > 0:
        return str_base(d, base) + digit_to_char(m)
    return digit_to_char(m)

def random_date(start, end):
    """
    This function will return a random datetime between two datetime 
    objects.
    """
    delta = end - start
    int_delta = (delta.days * 24 * 60 * 60) + delta.seconds
    random_second = random.randrange(int_delta)
    return start + timedelta(seconds=random_second)

SN_GEN_SIEM = lambda: str_base(random.random(), 36)[2:10]
SN_GEN_GE = lambda: str_base(random.random(), 10)[2:12]

ENGINEERS = ["J Nole", "A McFadden", "G Kemp", "B Wilson", "B Gee", "P Graham"]
MODELS = [
  { "name": "S-5000C",  "manufacturer": "SIEM" },
  { "name": "S-5000B",  "manufacturer": "SIEM" },
  { "name": "QQ-19",  "manufacturer": "GE" },
  { "name": "QQ-23", "manufacturer": "GE" },
]
SERIAL_NO_GEN = {
  "SIEM": SN_GEN_SIEM,
  "GE": SN_GEN_GE
}

def generate_assets(asset_coords):
  assets = {}
  for i in range(50):
    model = random.choice(MODELS)
    location = random.choice(asset_coords)
    id = "AB" + str(random.randrange(90000) + 10000)
    assets[id] = {
      "id": id,
      "clazz": "Signal",
      "model": model,
      "serial": SERIAL_NO_GEN[model["manufacturer"]](),
      "purchaseDate": random_date(datetime(2003, 1, 1), datetime(2013, 1, 1)),
      "lastInspectionDate": random_date(datetime(2016, 1, 1), datetime(2017, 1, 1)),
      "lastInspectionSignoff": random.choice(ENGINEERS),
      "retirementDate": random_date(datetime(2018, 1, 1), datetime(2020, 1, 1)),
      "location": {
        "lon": location[0],
        "lat": location[1]
      },
      "notes": [
           { 
             "id": "123", 
             "created": datetime(2017, 1, 1), 
             "text": "Please check that control circuit asset # sticker matches expected before performing maintenance." 
          }
       ],
       "events": [
         { "type": "maintenance", "date": datetime(2016, 9, 10) }, 
         { "type": "maintenance", "date": datetime(2017, 1, 3) },
         { "type": "failure", "date": datetime(2017, 1, 28) }
       ]
    }
  return assets

def generate_insights(assets):
  insights = []
  for i in range(10):
    number_assets = 10
    assets = random.sample(list(assets), number_assets)
    unfailed_assets = random.sample(assets, 4)
    zero_to_five = random.randrange(number_assets - 2)
    five_to_ten = random.randrange(number_assets - zero_to_five)
    greater_than_ten = random.randrange(number_assets - five_to_ten - zero_to_five)
    insights.append({
      "id": str(100 + i),
      "insightType": "cluster",
      "title": "Similar failures occurring in asset class",
      "subInsights": [
      {"icon": "pt-icon-info-sign", "text":"Statistically significant increase in failure rate following maintenance work"},
      {"icon": "pt-icon-info-sign", "text":"Baseline voltage of circuit diagnostic increased following maintenance activity"}
    ],
    "assetClass": "Signal",
    "assetIds": assets,
    "unfailedAssetIds": unfailed_assets,
    "barChart": {
      "data": [
        { "name": "0 -5 years", "value": zero_to_five },
        { "name": "5-10 years", "value": five_to_ten },
        { "name": "> 10 years", "value": greater_than_ten } 
      ],
      "xLabel": "# Failures",
      "yLabel": "Asset age"
    }
    })
  for i in range(110, 120):
    insights.append({
     "id": str(i),
     "insightType": "failure",
     "title": "Asset failure predicted",
     "assetIds": random.sample(list(assets), 1),
     "unfailedAssetIds": [],
     "assetClass": "Signal",
     "subInsights": [
       {"icon": "pt-icon-info-sign", "text": "An increase has been detected in the diagnostic voltage circuit"},
       {"icon": "pt-icon-info-sign", "text": "In 35% of previous occurences, this led to a failure within 2 weeks"}
     ],
     "events":[]
   })
  random.shuffle(insights)
  return insights


def make_feature(insight, asset):
  return {
        "type": "Feature",
        "geometry": {
          "type": "Point",
          "coordinates": [asset["location"]["lon"], asset["location"]["lat"]]
        },
        "properties": {
          "id": asset["id"],
          "View in Inbox": "http://localhost:3020/insights/{0}".format(insight["id"]),
          "Asset Class": asset["clazz"],
          "Model": asset["model"],
          "Serial": asset["serial"]
        }
  } 

if __name__ == "__main__":
  lines = json.load(open("data/linie.geojson"))
  switzerland = shape(json.load(open("data/switzerland.geojson"))["features"][0]["geometry"])
  asset_coords = [(f["geometry"]["coordinates"][0][0], f["geometry"]["coordinates"][0][1]) for f in lines["features"]]
  asset_coords.extend( [(f["geometry"]["coordinates"][1][0], f["geometry"]["coordinates"][1][1]) for f in lines["features"]])

  asset_coords = [coord for coord in asset_coords if switzerland.contains(Point(coord[0], coord[1]))]

  assets = generate_assets(asset_coords)
  json.dump(assets, open("data/assets.json", "w"), default=default, indent=1)

  insights = generate_insights(assets)
  json.dump(insights, open("data/insights.json", "w"), default=default, indent=1)

  cluster_insight = [insight for insight in insights if insight["insightType"] == "cluster"][0]
  insight_asset_ids = cluster_insight["unfailedAssetIds"]
  geojson = {
    "type": "FeatureCollection",
    "features": [make_feature(cluster_insight, assets[asset_id]) for asset_id in insight_asset_ids]
  }
  json.dump(geojson, open("data/failure-predictions.geojson", "w"), indent=1)
