import requests
import random
import time

API_ROOT = "http://localhost:8080/api"

if __name__ == "__main__":
    p = (0.1278, 51.5074)
    while True:
        p = (p[0] + random.random()*0.01, random.random()*0.01)


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
                    'featureCollection': geoj         
                    })
        print(r.text)
        time.sleep(5)
