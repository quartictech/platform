import * as React from "react";

const s = require("./style.css");

import { LatLon } from "../../models";

import SizeMe from "react-sizeme";

import mapboxgl from "./mapbox-gl-helper";

interface IMapProps {
  locations: LatLon[];
  colors: number[];
  width?: number;
  height: number;
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
    const lons = this.props.locations.map(l => l.lon);
    const lats = this.props.locations.map(l => l.lat);
    this.map = new mapboxgl.Map({
      container: "map-inner",
      style: "mapbox://styles/mapbox/bright-v9",
      zoom: 9.7,
      center: [lons[0], lats[0]],
    });
    this.map.on("load", () => {
      this.map.addLayer(circleLayer("points", this.props.locations, 8, "#ffffff"));
      this.map.addLayer(circleLayer("points2_0",
        this.props.locations.filter((_, idx) => this.props.colors[idx] === 0), 6, "#db3737"));
      this.map.addLayer(circleLayer("points2_1",
        this.props.locations.filter( (_, idx) => this.props.colors[idx] === 1), 6, "#0f9960"));
      this.map.fitBounds([[Math.min.apply(null, lons), Math.min.apply(null, lats)],
      [Math.max.apply(null, lons), Math.max.apply(null, lats)]
      ], { duration: 0, padding: 10 });
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

export const Map = SizeMe<IMapProps>()(RealMap); // tslint:disable-line:variable-name
