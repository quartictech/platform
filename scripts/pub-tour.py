#!/usr/bin/env python
import random
import time
import json
import csv
import os.path

import asyncio
import websockets



def random_message():
    return " ".join([
        random.choice(["@arlo", "@oliver", "@david.cameron"]),
        random.choice(["why are you such", "I wish you were more of", "don't you think I'm"]),
        random.choice(["an irritating person?", "an amazing guy?", "a dreamer?"]),
        random.choice(["I find that charming.", "I find that irritating.", "You go girl.", "I miss Jade Goodie ;-(", "Good for you mate!", "Jesus be praised!"])
        ])

def read_pubs(fname):
    pubs = []
    with open(fname) as f:
        for row in csv.reader(f):
            pubs.append({"name": row[0], "geojson": json.loads(row[1])})
    print("loaded {0} pubs".format(len(pubs)))
    return pubs

def delete_layer():
    requests.delete("{}/layer/live/{}".format(API_ROOT, LAYER_ID))

async def post_event(ws, user, geojson, message):
    await ws.send(json.dumps({
                    "timestamp": int(round(time.time() * 1000)),
                    "featureCollection": geojson,
                    "feedEvent": {
                        "source": user,
                        "message": message,
                    } if message else None
                    }))

def make_geojson(p, name, id):
    return {"type": "FeatureCollection", "features": [{
                "type": "Feature",
                "id": id,
                "geometry": {
                    "type": "Point",
                    "coordinates": p
                    },
                "properties": {
                    "name": name,
                    "timestamp": int(round(time.time() * 1000))
                    }
                }]}

STEPS = 50
pubs = read_pubs(os.path.join(os.path.dirname(__file__), "pubs.csv"))

async def socket(ws, path):
    p = (-0.1408, 51.5193)
    start = p
    target = None
    progress = 0
    noise_scale = 0.001
    arlo_noise_scale = noise_scale
    while True:
        if target is None or progress >= STEPS:
            progress = 0
            pub = random.choice(pubs)
            print("New target: {}".format(pub))
            target = pub["geojson"]["coordinates"]
            msg = "Heading to pub {}".format(pub["name"])
            await post_event(ws, "arlo", make_geojson(pub["geojson"]["coordinates"], "Arlo", pub["name"]), msg)
            start = p
            arlo_noise_scale *= 1.1

        dx, dy = target[0] - start[0], target[1] - start[1]
        scale = float(progress) / STEPS
        p = start[0] + (random.random() * noise_scale) + (scale * dx), start[1] + (random.random() * noise_scale) + (scale * dy)
        print("location: {}".format(p))

        await post_event(ws, "alex", make_geojson(p, "Alex", "alex"), random_message() if random.random() > 0.9 else None)
        await post_event(ws, "arlo", make_geojson((p[0] + random.random() * arlo_noise_scale, p[1] + random.random() * arlo_noise_scale), "Arlo", "arlo"), random_message() if random.random() > 0.99 else None)
        await asyncio.sleep(1)
        progress += 1


if __name__ == "__main__":
    start_server = websockets.serve(socket, 'localhost', 5000)
    asyncio.get_event_loop().run_until_complete(start_server)
    asyncio.get_event_loop().run_forever()
