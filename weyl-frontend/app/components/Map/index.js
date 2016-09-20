/**
*
* Map
*
*/

import React from "react";

import styles from "./styles.css";

import mapboxgl from "mapbox-gl";
import "mapbox-gl.css";
mapboxgl.accessToken = "pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw";

import SizeMe from "react-sizeme";
import { buildStyleLayers } from "./styles.js";

class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { map: null, sourceLayerMappings: {}, visibleLayerIds: [] };
  }

  onMouseMove(e) {
    const features = this.state.map.queryRenderedFeatures(e.point, { layers: this.state.visibleLayerIds });
    this.state.map.getCanvas().style.cursor = (features.length) ? "pointer" : "";
  }

  onMouseClick(e) {
    const features = this.state.map.queryRenderedFeatures(e.point, { layers: this.state.visibleLayerIds });
    if (!features.length) {
      return;
    }

    const feature = features[0];
    this.props.onSelectFeatures([[feature.layer.source, feature.properties._id]], features);
  }

  componentDidMount() {
    this.state.map = new mapboxgl.Map({
      container: "map-inner",
      style: this.props.map.style,
      zoom: 9.7,
      center: [-0.10, 51.4800],
    });

    this.state.map.on("mousemove", this.onMouseMove.bind(this));
    this.state.map.on("click", this.onMouseClick.bind(this));
    this.state.map.on("style.load", () => {
      this.props.onMapLoaded();
      this.updateState(this.props);
    });
  }


  createLayers(layer, styleLayers) {
    this.state.map.addSource(layer.id, {
      "type": "vector",
      "tiles": [`http://localhost:8080/api/${layer.id}/{z}/{x}/{y}.pbf`],
    });

    function zOrder(l) {
      return l.hasOwnProperty("_zorder") ? l._zorder : -1;
    }

    Object.keys(styleLayers)
      .sort((a, b) => zOrder(styleLayers[b]) - zOrder(styleLayers[a]))
      .forEach( styleLayerKey => {

        const styleLayer = {
          ...styleLayers[styleLayerKey],
          "id": `${layer.id}_${styleLayerKey}`,
          "source": layer.id,
          "source-layer": layer.id,
        };

        if (styleLayer.hasOwnProperty("_zorder")) {
          delete styleLayer["_zorder"];
        }

        this.state.map.addLayer(styleLayer);

        this.state.map.addLayer({
          "id": `${layer.id}_point_sel`,
          "type": "circle",
          "source": layer.id,
          "source-layer": layer.id,
          "paint": {
            "circle-color": "#FFB85F", // "#6e599f",
          },
          "filter": ["in", "_id", ""],
        });

        this.state.map.addLayer({
          "id": `${layer.id}_polygon_sel`,
          "type": "fill",
          "source": layer.id,
          "source-layer": layer.id,
          "paint": {
            "fill-color": "#FFB85F", // "#6e599f",
          },
          "filter": ["in", "_id", ""],
        });

        this.state.map.addLayer({
          "id": `${layer.id}_line_sel`,
          "type": "line",
          "source": layer.id,
          "source-layer": layer.id,
          "paint": {
            "line-color": "#FFB85F", // "#6e599f",
          },
          "filter": ["in", "_id", ""],
        });
      });

    this.state.sourceLayerMappings[layer.id] = Object.keys(styleLayers)
      .map(k => `${layer.id}_${k}`);
  }

  createValueFilter(spec) {
    const filter = ["none"];
    for (const attribute in spec) {
      const values = spec[attribute];

      if (values.length > 0) {
        const partial = values.reduce((f, v) => {
          f.push(v);
          return f;
        }, ["in", attribute]);

        filter.push(partial);
      }
    }
    return filter;
  }

  createSelectionFilter(selection, layerId) {
    if (selection.hasOwnProperty(layerId)) {
      return selection[layerId].reduce((f, v) => {
        f.push(v);
        return f;
      }, ["in", "_id"]);
    }
    return ["in", "_id", ""];
  }

  updateState(props) {
    this.state.visibleLayerIds = [];
    props.layers.forEach((layer) => {
      const styleLayers = buildStyleLayers(layer.name, layer.style, layer.stats.attributeStats);
      if (layer.visible) {
        Object.keys(styleLayers).forEach(k => this.state.visibleLayerIds.push(`${layer.id}_${k}`));
      }

      if (this.state.map.getSource(layer.id) === undefined) {
        this.createLayers(layer, styleLayers);
      }

      const valueFilter = this.createValueFilter(layer.filter);
      Object.keys(styleLayers).forEach(k => {
        Object.keys(styleLayers[k].paint).forEach(paintProperty =>
          this.state.map.setPaintProperty(`${layer.id}_${k}`, paintProperty, styleLayers[k].paint[paintProperty])
        );


        this.state.map.setLayoutProperty(`${layer.id}_${k}`, "visibility", layer.visible ? "visible" : "none");
        this.state.map.setLayoutProperty(`${layer.id}_point_sel`, "visibility", layer.visible ? "visible" : "none");
        this.state.map.setLayoutProperty(`${layer.id}_polygon_sel`, "visibility", layer.visible ? "visible" : "none");
        this.state.map.setLayoutProperty(`${layer.id}_polygon_sel`, "visibility", layer.visible ? "visible" : "none");

        if (styleLayers[k].hasOwnProperty("filter")) {
          const layerFilter = styleLayers[k].filter;
          this.state.map.setFilter(`${layer.id}_${k}`, ["all", valueFilter, layerFilter]);
        }
      });

      const selectionFilter = this.createSelectionFilter(props.selection, layer.id);
      this.state.map.setFilter(`${layer.id}_point_sel`, ["all", ["==", "$type", "Point"], selectionFilter, valueFilter]);
      this.state.map.setFilter(`${layer.id}_polygon_sel`, ["all", ["==", "$type", "Polygon"], selectionFilter, valueFilter]);
      this.state.map.setFilter(`${layer.id}_line_sel`, ["all", ["==", "$type", "LineString"], selectionFilter, valueFilter]);
    });
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.map.style !== this.props.map.style) {
      this.props.onMapLoading();
      this.state.map.setStyle(nextProps.map.style);
    } else if (nextProps.map.ready) {
      // Drawing before the map is ready causes sadness (this prop is set indirectly via the MapBox 'style.load' callback)
      this.updateState(nextProps);
    }
  }

  render() {
    return (
      <div className={styles.map}>
        <div id="map-inner" className={styles.mapViewport}>
        </div>
      </div>
    );
  }
}

export default SizeMe({ monitorHeight: true })(Map);
