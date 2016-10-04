/**
*
* Map
*
*/

import React from "react";

import styles from "./styles.css";

import mapboxgl from "./mapbox-gl-helper.js";
mapboxgl.accessToken = "pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw";

// TODO: there is some quite special magic going on here that throws eslint
import { Draw } from "mapbox-gl-draw/dist/mapbox-gl-draw";  // eslint-disable-line no-unused-vars
import "mapbox-gl-draw/dist/mapbox-gl-draw.css";

import SizeMe from "react-sizeme";
import { buildStyleLayers } from "./styles.js";
import { themes } from "../../themes";
import { apiRootUrl } from "../../utils.js";

// https://gist.github.com/samgiles/762ee337dff48623e729
Array.prototype.flatMap = function (lambda) {  // eslint-disable-line no-extend-native
  return Array.prototype.concat.apply([], this.map(lambda));
};

class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      map: null,
      zOrderedLayers: [],
      mappings: {},
    };
  }

  getVisibleSubLayers() {
    const visibleLayerIds = this.props.layers.filter(l => l.visible).map(l => l.id);
    return Object.keys(this.state.mappings)
      .filter(id => visibleLayerIds.some(i => i === id))
      .flatMap(id => this.state.mappings[id]);
  }

  queryRenderedFeatures(point) {
    return this.state.map.queryRenderedFeatures(point, { layers: this.getVisibleSubLayers() });
  }

  onMouseMove(e) {
    const features = this.queryRenderedFeatures(e.point);
    this.state.map.getCanvas().style.cursor = (features.length) ? "pointer" : "";
    this.props.onMouseMove(e.lngLat);
  }

  onMouseClick(e) {
    const features = this.queryRenderedFeatures(e.point);
    if (!features.length) {
      return;
    }

    const feature = features[0];
    this.props.onSelectFeatures([[feature.layer.source, feature.properties["_id"]]], features); // eslint-disable-line dot-notation
  }

  addLayer(layer, before) {
    this.state.map.addLayer(layer, before);
    this.state.zOrderedLayers.push(layer.id);
  }

  getBottomLayer = () => (this.state.zOrderedLayers.length > 0 ? this.state.zOrderedLayers[1] : null);

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

    this.state.map.on("draw.create", () => {
      this.props.onGeofenceChange(this.state.draw.getAll());
    });

    this.state.map.on("draw.delete", () => {
      this.props.onGeofenceChange(this.state.draw.getAll());
    });

    this.state.draw = mapboxgl.Draw({   // eslint-disable-line new-cap
      position: "top-right",
      displayControlsDefault: false,
      controls: {
        polygon: true,
        trash: true,
      },
    });
  }

  updateMap(props) {
    if (props.geofence.editing !== this.props.geofence.editing) {
      if (props.geofence.editing) {
        this.state.map.addControl(this.state.draw);
        this.props.onGeofenceChange(this.state.draw.getAll());
      } else {
        this.state.draw.remove();
      }
    }

    const mappings = Object.assign({}, this.state.mappings);

    props.layers.forEach(layer => {
      const sourceDef = (layer.live)
        ? this.getSourceDefForLiveLayer(layer)
        : this.getSourceDefForStaticLayer(layer);

      const x = this.updateLayer(layer, sourceDef, props);
      if (x !== undefined) {
        mappings[layer.id] = x;
      }
    });

    this.setState({ ...this.state, mappings });
    this.renderGeofence(props);
  }

  renderGeofence(props) {
    if (this.state.map.getSource("geofence") === undefined) {
      this.state.map.addSource("geofence", {
        type: "geojson",
        data: props.geofence.geojson,
      });
      this.addLayer({
        "id": "geofence_fill",
        "type": "fill",
        "source": "geofence",
        "source-layer": "geofence",
        "paint": {
          "fill-color": "#000000",
          "fill-opacity": 0.7,
        },
      }, this.getBottomLayer());

      this.addLayer({
        "id": "geofence_line",
        "type": "line",
        "source": "geofence",
        "source-layer": "geofence",
        "paint": {
          "line-color": "#000000",
          "line-width": 5,
        },
      }, this.getBottomLayer());
    }
    this.state.map.getSource("geofence").setData(props.geofence.geojson);
    const visible = props.geofence.geojson != null && !props.geofence.editing;
    this.setSubLayerVisibility("geofence_fill", visible);
    this.setSubLayerVisibility("geofence_line", visible);
    const geofenceColor = props.geofence.type === "INCLUDE" ? "#86C67C" : "#CC3300";
    this.state.map.setPaintProperty("geofence_fill", "fill-color", geofenceColor);
    this.state.map.setPaintProperty("geofence_line", "line-color", geofenceColor);
  }

  getSourceDefForStaticLayer(layer) {
    return {
      type: "vector",
      tiles: [`${apiRootUrl}/${layer.id}/{z}/{x}/{y}.pbf`],
    };
  }

  getSourceDefForLiveLayer(layer) {
    return {
      type: "geojson",
      data: layer.data,
    };
  }

  updateLayer(layer, sourceDef, props) {
    const styleLayers = buildStyleLayers(layer);

    var mappings = undefined;
    if (this.state.map.getSource(layer.id) === undefined) {
      mappings = this.createSourceAndSubLayers(layer, sourceDef, styleLayers);
    }
    if (layer.live) {
      this.state.map.getSource(layer.id).setData(sourceDef.data);
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

    return mappings;
  }

  addSubLayers(sourceId, subLayerDefs) {
    const finalDefs = subLayerDefs.map(def => ({
      ...def,
      "id": `${sourceId}_${def.id}`,
      "source": sourceId,
      "source-layer": sourceId,
    }));
    finalDefs.forEach(def => this.addLayer(def));
    return finalDefs.map(def => def.id);
  }

  createSourceAndSubLayers(layer, source, styleLayers) {
    this.state.map.addSource(layer.id, source);

    function zOrder(l) {
      return ("_zorder" in l) ? l["_zorder"] : -1;  // eslint-disable-line dot-notation
    }

    const subLayerDefs = [];

    Object.keys(styleLayers)
      .sort((a, b) => zOrder(styleLayers[b]) - zOrder(styleLayers[a]))
      .forEach(styleLayerKey => {
        const styleLayer = {
          ...styleLayers[styleLayerKey],
          "id": styleLayerKey,
        };

        if ("_zorder" in styleLayer) {
          delete styleLayer["_zorder"]; // eslint-disable-line dot-notation
        }

        subLayerDefs.push(styleLayer);
      });

    subLayerDefs.push({
      "id": "point_sel",
      "type": "circle",
      "paint": {
        "circle-color": "#FFB85F", // "#6e599f",
      },
      "filter": ["in", "_id", ""],
    });

    subLayerDefs.push({
      "id": "polygon_sel",
      "type": "fill",
      "paint": {
        "fill-color": "#FFB85F", // "#6e599f",
      },
      "filter": ["in", "_id", ""],
    });

    subLayerDefs.push({
      "id": "line_sel",
      "type": "line",
      "paint": {
        "line-color": "#FFB85F", // "#6e599f",
      },
      "filter": ["in", "_id", ""],
    });

    return this.addSubLayers(layer.id, subLayerDefs);
  }

  configureSubLayer(id, visible, valueFilter, config) {
    Object.keys(config.paint).forEach(paintProperty =>
      this.state.map.setPaintProperty(id, paintProperty, config.paint[paintProperty])
    );

    this.setSubLayerVisibility(id, visible);

    this.state.map.setFilter(id, ("filter" in config)
      ? ["all", valueFilter, config.filter]
      : valueFilter
    );
  }

  createValueFilter(spec) {
    return ["none"].concat(
      Object.keys(spec)
        .filter(k => spec[k].length > 0)
        .map(k => ["in", k].concat(spec[k]))
    );
  }

  createSelectionFilter(selection, layerId) {
    return ["in", "_id"].concat((layerId in selection) ? selection[layerId] : "");
  }

  setSubLayerVisibility(id, visible) {
    this.state.map.setLayoutProperty(id, "visibility", visible ? "visible" : "none");
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.map.theme !== this.props.map.theme) {
      this.props.onMapLoading();
      this.state.map.setStyle(themes[nextProps.map.theme].mapbox);
    } else if (nextProps.map.ready) {
      // Drawing before the map is ready causes sadness (this prop is set indirectly via the MapBox "style.load" callback)
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

export default SizeMe({ monitorHeight: true })(Map);  // eslint-disable-line new-cap
