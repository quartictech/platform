import * as constants from "./constants";

export function search(query, callback) {
  console.log("Search started");
  return {
    type: constants.SEARCH,
    query,
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

export const layerSetStyle = (layerId, key, value) => ({
  type: constants.LAYER_SET_STYLE,
  layerId,
  key,
  value,
});

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

export function layerSetData(layerId, data, schema) {
  console.assert(layerId != null);
  return {
    type: constants.LAYER_SET_DATA,
    layerId,
    data,
    schema,
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

export const mapSetLocation = (location) => ({
  type: constants.MAP_SET_LOCATION,
  location,
});

export function geofenceEditStart() {
  return {
    type: constants.GEOFENCE_EDIT_START,
  };
}

export const geofenceEditFinish = () => ({
  type: constants.GEOFENCE_EDIT_FINISH,
});

export const geofenceSetGeometry = (geojson) => ({
  type: constants.GEOFENCE_SET_GEOMETRY,
  geojson,
});

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

export function geofenceSetLayer(layerId, bufferDistance) {
  return {
    type: constants.GEOFENCE_SET_LAYER,
    layerId,
    bufferDistance,
  };
}

export const geofenceSetViolatedGeofences = (violatedIds) => ({
  type: constants.GEOFENCE_SET_VIOLATED_GEOFENCES,
  violatedIds,
});

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

export const selectionInfoLoading = () => ({
  type: constants.SELECTION_INFO_LOADING,
});

export const selectionInfoLoaded = (results) => ({
  type: constants.SELECTION_INFO_LOADED,
  results,
});

export const selectionInfoFailedToLoad = () => ({
  type: constants.SELECTION_INFO_FAILED_TO_LOAD,
});
