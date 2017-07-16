import * as React from "react";
import {
  Colors,
} from "@blueprintjs/core";
const s = require("./style.css");

// import mapboxgl from "./mapbox-gl-helper";
import mapbox = require("mapbox-gl/dist/mapbox-gl.js");
import * as mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
mapbox.accessToken = "pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw";

import geojsonExtent = require("@mapbox/geojson-extent");

export type MapStyle = "basic" | "streets" | "bright" | "light" | "dark" | "satellite";

interface Props {
  featureCollection: GeoJSON.FeatureCollection<GeoJSON.GeometryObject>;
  width?: number;
  height: number;
  style?: MapStyle;
}

class Map extends React.Component<Props, any> {
  static defaultProps: Partial<Props> = {
    style: "bright",
  };

  private map: mapboxgl.Map;

  constructor() {
    super();
  }

  componentDidMount() {
    this.map = new mapboxgl.Map({
      container: "map-inner",
      style: mapboxStyleUri(this.props.style),
      zoom: 9.7,
    });

    this.map.on("style.load", () => {
      this.configureMap();
    });
  }

  render() {
    return (
      <div className={s.map} style={{ height: this.props.height }}>
        <div id="map-inner" className={s.mapViewport} style={{ width: "100%", height: "100%" }}>
        </div>
      </div>
    );
  }

  componentWillUpdate(nextProps: Props) {
    this.updateData(nextProps.featureCollection);
    if (nextProps.style !== this.props.style) {
      this.map.setStyle(mapboxStyleUri(nextProps.style));
    }
  }

  private configureMap() {
    this.map.addSource("geojson", { type: "geojson", data: { type: "FeatureCollection", features: [] } });
    this.map.addLayer({
      id: "line",
      type: "line",
      source: "geojson",
      paint: {
        "line-width": 5,
        "line-color": (this.props.style === "satellite") ? Colors.GOLD3 : Colors.GREEN3,
      },
    });
    this.updateData(this.props.featureCollection);
  }

  private updateData(featureCollection: GeoJSON.FeatureCollection<GeoJSON.GeometryObject>) {
    const source: mapboxgl.GeoJSONSource = this.map.getSource("geojson") as mapboxgl.GeoJSONSource;
    if (source != null) {
      source.setData(featureCollection);

      const e = geojsonExtent(featureCollection);
      const options = { padding: 30 , animate: false };
      this.map.fitBounds(e, options);
    }
  }
}

const mapboxStyleUri = (style: MapStyle) => `mapbox://styles/mapbox/${style}-v9`;

export default Map;
