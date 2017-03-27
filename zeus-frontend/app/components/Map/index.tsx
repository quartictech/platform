import * as React from "react";

const s = require("./style.css");

import { ILatLon } from "../../models";

import SizeMe from "react-sizeme";

import mapboxgl from "./mapbox-gl-helper";

interface IMapProps {
  locations: ILatLon[];
  width: number;
}

const circleLayer = (id, locations, width, color) => ({
      "id": id,
      "type": "circle",
      "source": {
          "type": "geojson",
          "data": {
              "type": "FeatureCollection",
              "features": locations.map(loc => ({
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [loc.lon, loc.lat]
                  }
              }))
          }
      },
      "paint": {
        "circle-radius": width,
        "circle-color": color,
      }
});

class RealMap  extends React.Component<IMapProps, any> {
  map: any;

  constructor() {
    super();
  }



  componentDidMount() {
    const centerLon = this.props.locations.map(l => l.lon).reduce((a, b) => a + b, 0)  / this.props.locations.length;
    const centerLat = this.props.locations.map(l => l.lat).reduce((a, b) => a + b, 0)  / this.props.locations.length;
    this.map = new mapboxgl.Map({
      container: "map-inner",
      style: "mapbox://styles/mapbox/bright-v9",
      zoom: 9.7,
      center: [centerLon, centerLat],
    });
    this.map.on('load', () => {
      this.map.addLayer(circleLayer("points", this.props.locations, 8, "#ffffff"));
      this.map.addLayer(circleLayer("points2", this.props.locations, 6, "#e7298a"));
    });
  }

  render() {
    return (
      <div className={s.map}>
        <div id="map-inner" className={s.mapViewport} style={{width: this.props.width, height: 200}}>
        </div>
      </div>
    );
  }
}

export const Map = SizeMe()(RealMap);
