import React from "react";

import styles from "./styles.css";

import mapboxgl from "./mapbox-gl-helper.js";

// TODO: there is some quite special magic going on here that throws eslint
import { Draw } from "mapbox-gl-draw/dist/mapbox-gl-draw";  // eslint-disable-line no-unused-vars
import "mapbox-gl-draw/dist/mapbox-gl-draw.css";

import SizeMe from "react-sizeme";
import { buildStyleLayers } from "./styles.js";
import { mapThemes } from "../../themes";
import { apiRootUrl, mapboxToken } from "../../utils.js";
mapboxgl.accessToken = mapboxToken;

const _ = require("underscore");


class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.map = null;
    this.subLayers = {};
  }

  onMouseMove(e) {
    const features = this.queryRenderedFeatures(e.point);
    this.map.getCanvas().style.cursor = (features.length) ? "pointer" : "";
    this.props.onMouseMove(e.lngLat);
  }

  onMouseClick(e) {
    const features = this.queryRenderedFeatures(e.point);
    const feature = (features.length > 0)
      ? {
        id: features[0].properties["_id"],  // eslint-disable-line dot-notation
        externalId: features[0].properties["_externalId"], // eslint-disable-line dot-notation
        entityId: features[0].properties["_entityId"], // eslint-disable-line dot-notation
        layerId: features[0].layer.source,
        properties: features[0].properties,
      }
      : undefined;
    this.props.onMouseClick(feature, e.originalEvent.metaKey);
  }

  queryRenderedFeatures(point) {
    return this.map.queryRenderedFeatures(point, { layers: this.getVisibleSubLayers() });
  }

  getVisibleSubLayers() {
    const visibleLayerIds = _.values(this.props.layers)
      .filter(l => l.visible)
      .map(l => l.id);
    return _.flatten(Object.keys(this.subLayers)
      .filter(id => visibleLayerIds.some(i => i === id))
      .map(id => this.subLayers[id]));
  }

  componentDidMount() {
    this.map = new mapboxgl.Map({
      container: "map-inner",
      style: mapThemes[this.props.map.theme].mapbox,
      zoom: 9.7,
      center: [-0.10, 51.4800],
    });

    this.map.dragRotate.disable();

    this.map.on("mousemove", this.onMouseMove.bind(this));
    this.map.on("click", this.onMouseClick.bind(this));
    this.map.on("style.load", () => {
      this.subLayers = {};     // Everything is gone once the style changes
      this.props.onMapLoaded();
      this.updateMap(this.props);
    });

    this.map.on("draw.create", () => {
      this.props.onGeofenceSetGeometry(this.draw.getAll());
    });

    this.map.on("draw.delete", () => {
      this.props.onGeofenceSetGeometry(this.draw.getAll());
    });

    this.draw = mapboxgl.Draw({   // eslint-disable-line new-cap
      position: "top-right",
      displayControlsDefault: false,
      controls: {
        polygon: true,
        trash: true,
      },
    });
  }

  updateMap(props) {
    this.updateNonIdempotent(props);
    this.updateIdempotent(props);
  }

  updateNonIdempotent(props) {
    this.toggleGeofenceEditControlsIfNeeded(props);
    this.addGeofenceLayerIfMissing(props.geofence);
    this.deleteOldLayers(props.layers);
    this.addNewLayers(props.layers);
  }

  updateIdempotent(props) {
    this.updateLayers(props.layers, props.selection);
    this.updateGeofenceLayer(props.geofence);
  }

  toggleGeofenceEditControlsIfNeeded(props) {
    if (props.geofence.editing !== this.props.geofence.editing) {
      if (props.geofence.editing) {
        this.map.addControl(this.draw);
        this.props.onGeofenceSetGeometry(this.draw.getAll());
      } else {
        this.draw.remove();
      }
    }
  }

  addGeofenceLayerIfMissing(geofence) {
    if (this.map.getSource("geofence") === undefined) {
      this.map.addSource("geofence", {
        type: "geojson",
        data: geofence.geojson,
      });

      this.addSubLayers("geofence", [
        {
          "id": "fill",
          "type": "fill",
          "paint": {
            "fill-color": "#86C67C",
            "fill-opacity": 0.7,
          },
        },
        {
          "id": "line",
          "type": "line",
          "paint": {
            "line-color": "#86C67C",
            "line-width": 5,
          },
        },
        {
          "id": "fill_violated",
          "type": "fill",
          "paint": {
            "fill-color": "#CC3300",
            "fill-opacity": 0.7,
          },
        },
        {
          "id": "line_violated",
          "type": "line",
          "paint": {
            "line-color": "#CC3300",
            "line-width": 5,
          },
        },
      ]);
    }
  }

  updateGeofenceLayer(geofence) {
    this.map.getSource("geofence").setData(geofence.geojson);

    const visible = !geofence.editing;
    this.setSubLayerVisibility("geofence_fill", visible);
    this.setSubLayerVisibility("geofence_line", visible);
    this.setSubLayerVisibility("geofence_fill_violated", visible);
    this.setSubLayerVisibility("geofence_line_violated", visible);

    const unviolatedFilter = ["!in", "_externalId"].concat(geofence.violatedIds);
    const violatedFilter = ["in", "_externalId"].concat(geofence.violatedIds);
    this.map.setFilter("geofence_fill", unviolatedFilter);
    this.map.setFilter("geofence_line", unviolatedFilter);
    this.map.setFilter("geofence_fill_violated", violatedFilter);
    this.map.setFilter("geofence_line_violated", violatedFilter);
  }

  deleteOldLayers(layers) {
    const layerIdsToDelete = _.keys(this.subLayers).filter(id => !(id in layers));

    layerIdsToDelete.forEach(id => {
      this.subLayers[id].forEach(subLayerId => this.map.removeLayer(subLayerId));
      this.map.removeSource(id);
      delete this.subLayers[id];
    });
  }

  addNewLayers(layers) {
    const layerIdsToAdd = _.keys(layers).filter(id => !(id in this.subLayers));

    layerIdsToAdd.forEach(id => {
      const subLayerIds = this.createSourceAndSubLayers(layers[id]);
      this.subLayers[id] = subLayerIds;
    });
  }

  updateLayers(layers, selection) {
    _.values(layers).forEach(layer => this.updateLayer(layer, selection));
  }

  updateLayer(layer, selection) {
    if (layer.live) {
      this.map.getSource(layer.id).setData(this.getSourceDef(layer).data);
    }

    const styleLayers = buildStyleLayers(layer);
    const valueFilter = this.createValueFilter(layer.filter);
    Object.keys(styleLayers).forEach(k => {
      this.configureSubLayer(`${layer.id}_${k}`, layer.visible, valueFilter, styleLayers[k]);
    });

    this.setSubLayerVisibility(`${layer.id}_point_sel`, layer.visible);
    this.setSubLayerVisibility(`${layer.id}_line_sel`, layer.visible);
    this.setSubLayerVisibility(`${layer.id}_polygon_sel`, layer.visible);
    const selectionFilter = this.createSelectionFilter(selection, layer.id);
    this.map.setFilter(`${layer.id}_point_sel`, ["all", ["==", "$type", "Point"], selectionFilter, valueFilter]);
    this.map.setFilter(`${layer.id}_polygon_sel`, ["all", ["==", "$type", "Polygon"], selectionFilter, valueFilter]);
    this.map.setFilter(`${layer.id}_line_sel`, ["all", ["==", "$type", "LineString"], selectionFilter, valueFilter]);
  }

  getSourceDef(layer) {
    return (layer.live)
      ? { type: "geojson", data: layer.data }
      : { type: "vector", tiles: [`${apiRootUrl}/${layer.id}/{z}/{x}/{y}.pbf`] };
  }

  createSourceAndSubLayers(layer) {
    this.map.addSource(layer.id, this.getSourceDef(layer));

    function zOrder(l) {
      return ("_zorder" in l) ? l["_zorder"] : -1;  // eslint-disable-line dot-notation
    }

    const styleLayers = buildStyleLayers(layer);

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

  addSubLayers(sourceId, subLayerDefs) {
    const finalDefs = subLayerDefs.map(def => ({
      ...def,
      "id": `${sourceId}_${def.id}`,
      "source": sourceId,
      "source-layer": sourceId,
    }));
    finalDefs.forEach(def => this.map.addLayer(def));
    return finalDefs.map(def => def.id);
  }

  configureSubLayer(id, visible, valueFilter, config) {
    Object.keys(config.paint).forEach(paintProperty =>
      this.map.setPaintProperty(id, paintProperty, config.paint[paintProperty])
    );

    this.setSubLayerVisibility(id, visible);

    this.map.setFilter(id, ("filter" in config)
      ? ["all", valueFilter, config.filter]
      : valueFilter
    );
  }

  createValueFilter(spec) {
    return ["none"].concat(
      Object.keys(spec)
        .filter(k => (spec[k].categories.length > 0 || spec[k].notApplicable))
        .map(k => {
          const catFilter = ["in", k].concat(spec[k].categories);
          return spec[k].notApplicable ? ["any", ["!has", k], catFilter] : catFilter;
        })
    );
  }

  createSelectionFilter(selection, layerId) {
    return ["in", "_entityId"].concat((layerId in selection) ? selection[layerId] : "");
  }

  setSubLayerVisibility(id, visible) {
    this.map.setLayoutProperty(id, "visibility", visible ? "visible" : "none");
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.map.targetLocation !== this.props.map.targetLocation) {
      this.map.flyTo({ center: nextProps.map.targetLocation, zoom: 16 });
    }

    if (nextProps.map.theme !== this.props.map.theme) {
      this.props.onMapLoading();
      this.map.setStyle(mapThemes[nextProps.map.theme].mapbox);
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
