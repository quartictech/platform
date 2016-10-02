import requests
import random
import time

API_ROOT = "http://localhost:8080/api"

def random_message():
    return " ".join([
        random.choice(["@arlo", "@oliver", "@david.cameron"]),
        random.choice(["why are you such", "I wish you were more of", "don't you think I'm"]),
        random.choice(["an irritating person?", "an amazing guy?", "a dreamer?"]),
        random.choice(["I find that charming.", "I find that irritating.", "You go girl.", "I miss Jade Goodie ;-("])
        ])

if __name__ == "__main__":
    p = (0.1278, 51.5074)
    while True:
        p = (p[0] + random.random()*0.01, p[1] + random.random()*0.01)


        geoj = {"type": "FeatureCollection", "features": [{
                "type": "Feature",
                "id": "alex",
                "geometry": {
                    "type": "Point",
                    "coordinates": p
                    },
                "properties": {
                    "name": "Alex",
                    "timestamp": int(round(time.time() * 1000))
                    }
                }]}
        r = requests.post("{}/layer/live/{}".format(API_ROOT, "alex-test"),
                json={
                    'name': 'Alex Test',
                    'description': 'A weird gimpy little test',
                    'viewType': 'LOCATION_AND_TRACK',
                    'events': [
                        {
                            "timestamp": int(round(time.time() * 1000)),
                            "featureCollection": geoj,
                            "feedEvent": {
                                "source": "alex",
                                "message": random_message(),
                            }
                        }
                    ]}
                    )
        print(r.text)
        time.sleep(5)
