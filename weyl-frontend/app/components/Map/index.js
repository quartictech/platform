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
import { themes } from "../../themes";

class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { map: null, sourceLayerMappings: {}, visibleLayerIds: [] };
  }

  onMouseMove(e) {
    const features = this.state.map.queryRenderedFeatures(e.point, { layers: this.state.visibleLayerIds });
    this.state.map.getCanvas().style.cursor = (features.length) ? "pointer" : "";
    this.props.onMouseMove(e.lngLat);
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
      style: themes[this.props.map.theme].mapbox,
      zoom: 9.7,
      center: [-0.10, 51.4800],
    });

    this.state.map.on("mousemove", this.onMouseMove.bind(this));
    this.state.map.on("click", this.onMouseClick.bind(this));
    this.state.map.on("style.load", () => {
      this.props.onMapLoaded();
      this.updateMap(this.props);
    });

    this.installRefreshCallback();
  }

  installRefreshCallback() {
    const owner = this;
    window.setInterval(function() {
      owner.props.layers
        .filter(layer => layer.live)
        .forEach(layer => {
          const source = owner.state.map.getSource(layer.id);
          const data = source["_data"];
          source.setData(data);
        })
      }, 250);
  }

  updateMap(props) {
    this.state.visibleLayerIds = [];
    const owner = this;

    props.layers.filter(layer => layer.live).forEach(layer => {
      this.updateLayer(layer, this.getSourceDefForLiveLayer(layer.id), props);
    });

    props.layers.filter(layer => !layer.live).forEach(layer =>
      this.updateLayer(layer, this.getSourceDefForStaticLayer(layer.id), props)
    );
  }

  addLiveLayer(props) {
    const layer = {
      name: "weirdness",
      // style: XXX,
      visible: true,
    }
  }

  getSourceDefForStaticLayer(id) {
    return {
      type: "vector",
      tiles: [`http://localhost:8080/api/${id}/{z}/{x}/{y}.pbf`],
    };
  }

  getSourceDefForLiveLayer(id) {
    return {
      type: 'geojson',
      data: `http://localhost:8080/api/layer/live/${id}`,
    };
  }

  updateLayer(layer, sourceDef, props) {
    const styleLayers = buildStyleLayers(layer.name, layer.style, layer.stats.attributeStats);

    if (this.state.map.getSource(layer.id) === undefined) {
      this.createSourceAndSubLayers(layer, sourceDef, styleLayers);
    }

    const valueFilter = this.createValueFilter(layer.filter);
    Object.keys(styleLayers).forEach(k => {
      this.configureSubLayer(`${layer.id}_${k}`, layer.visible, valueFilter, styleLayers[k]);
    });

    this.setSubLayerVisibility(`${layer.id}_point_sel`, layer.visible);
    this.setSubLayerVisibility(`${layer.id}_line_sel`, layer.visible);
    this.setSubLayerVisibility(`${layer.id}_polygon_sel`, layer.visible);
    const selectionFilter = this.createSelectionFilter(props.selection, layer.id);
    this.state.map.setFilter(`${layer.id}_point_sel`, ["all", ["==", "$type", "Point"], selectionFilter, valueFilter]);
    this.state.map.setFilter(`${layer.id}_polygon_sel`, ["all", ["==", "$type", "Polygon"], selectionFilter, valueFilter]);
    this.state.map.setFilter(`${layer.id}_line_sel`, ["all", ["==", "$type", "LineString"], selectionFilter, valueFilter]);
  }

  createSourceAndSubLayers(layer, source, styleLayers) {
    this.state.map.addSource(layer.id, source);

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
      });

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

    this.state.sourceLayerMappings[layer.id] = Object.keys(styleLayers)
      .map(k => `${layer.id}_${k}`);
  }

  configureSubLayer(id, visible, valueFilter, config) {
    Object.keys(config.paint).forEach(paintProperty =>
      this.state.map.setPaintProperty(id, paintProperty, config.paint[paintProperty])
    );

    this.setSubLayerVisibility(id, visible);

    this.state.map.setFilter(id, config.hasOwnProperty("filter")
      ? ["all", valueFilter, config.filter]
      : valueFilter
    );
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

  setSubLayerVisibility(id, visible) {
    this.state.map.setLayoutProperty(id, "visibility", visible ? "visible" : "none");
    if (visible) {
      this.state.visibleLayerIds.push(id);
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.map.theme !== this.props.map.theme) {
      this.props.onMapLoading();
      this.state.map.setStyle(themes[nextProps.map.theme].mapbox);
    } else if (nextProps.map.ready) {
      // Drawing before the map is ready causes sadness (this prop is set indirectly via the MapBox 'style.load' callback)
      this.updateMap(nextProps);
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
