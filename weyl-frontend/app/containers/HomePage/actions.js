import * as constants from "./constants";

export function search(query, callback) {
  console.log("Search started");
  return {
    type: constants.SEARCH,
    query,
    callback,
  };
}

export function searchDone(results, callback) {
  return {
    type: constants.SEARCH_DONE,
    response: results,
    callback,
  };
}

export function layerCreate(result) {
  return {
    type: constants.LAYER_CREATE,
    id: result.id,
    metadata: result.metadata,
    stats: result.stats,
    attributeSchema: result.attributeSchema,
    live: result.live,
  };
}

export function layerSetStyle(layerId, style) {
  return {
    type: constants.LAYER_SET_STYLE,
    layerId,
    style,
  };
}

export function layerToggleValueVisible(layerId, attribute, value) {
  return {
    type: constants.LAYER_TOGGLE_VALUE_VISIBLE,
    layerId,
    attribute,
    value,
  };
}

export function layerToggleVisible(layerId) {
  console.assert(layerId != null);
  return {
    type: constants.LAYER_TOGGLE_VISIBLE,
    layerId,
  };
}

export function layerClose(layerId) {
  console.assert(layerId != null);
  return {
    type: constants.LAYER_CLOSE,
    layerId,
  };
}

export function layerSetData(layerId, data) {
  console.assert(layerId != null);
  return {
    type: constants.LAYER_SET_DATA,
    layerId,
    data,
  };
}

export function bucketComputation(computation) {
  console.log("Bucket computation");
  return {
    type: constants.BUCKET_COMPUTATION_START,
    computation,
  };
}

export function toggleUi(element) {
  return {
    type: constants.UI_TOGGLE,
    element,
  };
}

export function clearSelection() {
  return {
    type: constants.CLEAR_SELECTION,
  };
}

export function loadNumericAttributes(layerId) {
  return {
    type: constants.NUMERIC_ATTRIBUTES_LOAD,
    layerId,
  };
}

export function loadNumericAttributesDone(data) {
  return {
    type: constants.NUMERIC_ATTRIBUTES_LOADED,
    data,
  };
}

export function chartSelectAttribute(attribute) {
  return {
    type: constants.CHART_SELECT_ATTRIBUTE,
    attribute,
  };
}

export function mapLoading() {
  return {
    type: constants.MAP_LOADING,
  };
}

export function mapLoaded() {
  return {
    type: constants.MAP_LOADED,
  };
}

export function mapMouseMove(mouseLocation) {
  return {
    type: constants.MAP_MOUSE_MOVE,
    mouseLocation,
  };
}

export function mapMouseClick(feature, multiSelectEnabled) {
  return {
    type: constants.MAP_MOUSE_CLICK,
    feature,
    multiSelectEnabled,
  };
}

export function geofenceEditStart() {
  return {
    type: constants.GEOFENCE_EDIT_START,
  };
}

export function geofenceEditFinish(geofence) {
  return {
    type: constants.GEOFENCE_EDIT_FINISH,
    geofence,
  };
}

export function geofenceEditChange(geojson) {
  return {
    type: constants.GEOFENCE_EDIT_CHANGE,
    geojson,
  };
}

export function geofenceChangeType(geofenceType) {
  return {
    type: constants.GEOFENCE_CHANGE_TYPE,
    value: geofenceType,
  };
}

export function geofenceSaveDone() {
  return {
    type: constants.GEOFENCE_SAVE_DONE,
  };
}

export function feedSetData(layerId, data) {
  console.assert(layerId != null);
  return {
    type: constants.FEED_SET_DATA,
    layerId,
    data,
  };
}

export const connectionUp = () => ({
  type: constants.CONNECTION_UP,
});

export const connectionDown = () => ({
  type: constants.CONNECTION_DOWN,
});
