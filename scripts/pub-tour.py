#!/usr/bin/env python
import random
import time
import json
import csv
import os.path

import asyncio
import websockets



def read_pubs(fname):
    pubs = []
    with open(fname) as f:
        for row in csv.reader(f):
            pubs.append({"name": row[0], "geojson": json.loads(row[1])})
    print("loaded {0} pubs".format(len(pubs)))
    return pubs

def delete_layer():
    requests.delete("{}/layer/live/{}".format(API_ROOT, LAYER_ID))

async def post_event(ws, user, geojson):
    await ws.send(json.dumps({
                    "timestamp": int(round(time.time() * 1000)),
                    "featureCollection": geojson,
                    }))

def make_geojson(p, name, id, blood_alcohol=None):
    geojson = {"type": "FeatureCollection", "features": [{
                "type": "Feature",
                "id": id,
                "geometry": {
                    "type": "Point",
                    "coordinates": p
                    },
                "properties": {
                    "name": name,
                    "timestamp": int(round(time.time() * 1000)),
                    "_table": [
                        { "name": name, "bar": 123, "gimp": "yeah" },
                        { "name": name, "bar": 456, "gimp": "no" },
                        { "name": name, "bar": 789, "gimp": "maybe" },
                        { "name": name, "bar": 000, "gimp": "no" },
                        { "name": name, "bar": 111, "gimp": "maybe" },
                        { "name": name, "bar": 222, "gimp": "no" },
                        { "name": name, "bar": 333, "gimp": "maybe" },
                    ]
                    }
                    }]}
    if blood_alcohol:
        geojson["features"][0]["properties"]["Blood Alcohol Level"] = {
                            "type": "timeseries",
                            "series": blood_alcohol
                            }
    return geojson

STEPS = 50
pubs = read_pubs(os.path.join(os.path.dirname(__file__), "pubs.csv"))
async def socket(ws, path):
    p = (-0.1408, 51.5193)
    start = p
    target = None
    progress = 0
    noise_scale = 0.001
    arlo_noise_scale = noise_scale
    arlo_blood_alcohol = []
    alex_blood_alcohol = []
    arlo_bah = 0
    alex_bah = 0
    while True:
        if target is None or progress >= STEPS:
            progress = 0
            pub = random.choice(pubs)
            print("New target: {}".format(pub))
            target = pub["geojson"]["coordinates"]
            msg = "Heading to pub {}".format(pub["name"])
            await post_event(ws, "arlo", make_geojson(pub["geojson"]["coordinates"], "Arlo", pub["name"]))
            start = p
            arlo_noise_scale *= 1.1

        dx, dy = target[0] - start[0], target[1] - start[1]
        scale = float(progress) / STEPS
        p = start[0] + (random.random() * noise_scale) + (scale * dx), start[1] + (random.random() * noise_scale) + (scale * dy)
        print("location: {}".format(p))

        arlo_blood_alcohol.append({
            "timestamp": time.time()*1000,
            "value": arlo_bah
        })

        alex_blood_alcohol.append({
            "timestamp": time.time()*1000,
            "value": alex_bah
        })
        arlo_bah += random.random() * 0.05
        alex_bah += random.random() * 0.03
        if arlo_bah > 1: arlo_bah = 1
        if alex_bah > 1: alex_bah = 1
        await post_event(ws, "alex", make_geojson(p, "Alex", "alex", alex_blood_alcohol))
        await post_event(ws, "arlo",
            make_geojson((p[0] + random.random() * arlo_noise_scale,
                          p[1] + random.random() * arlo_noise_scale), "Arlo", "arlo", arlo_blood_alcohol))
        await asyncio.sleep(5)
        progress += 1


if __name__ == "__main__":
    start_server = websockets.serve(socket, 'localhost', 5000)
    asyncio.get_event_loop().run_until_complete(start_server)
    asyncio.get_event_loop().run_forever()
