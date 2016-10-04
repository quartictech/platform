#!/usr/bin/env python
import requests
import random
import time
import json
import csv
import os.path

API_ROOT = "http://localhost:8080/api"
LAYER_NAME = "pub-tour"

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
    requests.delete("{}/layer/live/{}".format(API_ROOT, LAYER_NAME))

def post_event(user, geojson, message):
    r = requests.post("{}/layer/live/{}".format(API_ROOT, LAYER_NAME),
        json={
            'name': 'Pub tour',
            'description': 'A weird gimpy little test',
            'icon': 'brown beer',
            'viewType': 'LOCATION_AND_TRACK',
            'events': [
                {
                    "timestamp": int(round(time.time() * 1000)),
                    "featureCollection": geojson,
                    "feedEvent": {
                        "source": user,
                        "message": message,
                    } if message else None
                }
            ]}
        )
    print(r.text)

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
if __name__ == "__main__":
    delete_layer()
    pubs = read_pubs(os.path.join(os.path.dirname(__file__), "pubs.csv"))
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
            post_event("arlo", make_geojson(pub["geojson"]["coordinates"], "Arlo", pub["name"]), msg)
            start = p
            arlo_noise_scale *= 1.1

        dx, dy = target[0] - start[0], target[1] - start[1]
        scale = float(progress) / STEPS
        p = start[0] + (random.random() * noise_scale) + (scale * dx), start[1] + (random.random() * noise_scale) + (scale * dy)
        print("location: {}".format(p))

        post_event("alex", make_geojson(p, "Alex", "alex"), random_message() if random.random() > 0.9 else None)
        post_event("arlo", make_geojson((p[0] + random.random() * arlo_noise_scale, p[1] + random.random() * arlo_noise_scale), "Arlo", "arlo"), random_message() if random.random() > 0.99 else None)
        time.sleep(1)
        progress += 1
