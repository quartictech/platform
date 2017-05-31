import * as React from "react";

const s = require("./style.css");

import SizeMe from "react-sizeme";

// import mapboxgl from "./mapbox-gl-helper";
import mapbox = require("mapbox-gl/dist/mapbox-gl.js");
import * as mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
mapbox.accessToken = "pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw";

import geojsonExtent = require("@mapbox/geojson-extent");

interface IMapProps {
  featureCollection: GeoJSON.FeatureCollection<GeoJSON.GeometryObject>;
  width?: number;
  height: number;
}

class RealMap  extends React.Component<IMapProps, any> {
  map: mapboxgl.Map;

  constructor() {
    super();
  }

  componentDidMount() {
    this.map = new mapboxgl.Map({
      container: "map-inner",
      style: "mapbox://styles/mapbox/bright-v9",
      zoom: 9.7,
    });

    this.map.on("load", () => {
      this.map.addSource("geojson", { type: "geojson", data: { type: "FeatureCollection", features: [] } });
      this.updateData(this.props.featureCollection);
      this.map.addLayer({
        id: "line",
        type: "line",
        source: "geojson",
        paint: {
          "line-width": 5,
          "line-color": "#0f9960",
        },
      });
    });
  }

  render() {
    return (
      <div className={s.map} style={{ height: this.props.height }}>
        <div id="map-inner" className={s.mapViewport} style={{width: "100%", height: "100%"}}>
        </div>
      </div>
    );
  }

  componentWillUpdate(nextProps: IMapProps) {
    this.updateData(nextProps.featureCollection);
  }

  updateData(featureCollection: GeoJSON.FeatureCollection<GeoJSON.GeometryObject>) {
    const source: mapboxgl.GeoJSONSource = this.map.getSource("geojson") as mapboxgl.GeoJSONSource;
    if (source != null) {
      source.setData(featureCollection);

      const e = geojsonExtent(featureCollection);
      const options = { padding: 30 , animate: false };
      this.map.fitBounds(e, options);
    }
  }
}

export const Map = SizeMe<IMapProps>()(RealMap); // tslint:disable-line:variable-name
