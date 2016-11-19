import * as constants from "./constants";

export function search(query, callback) {
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
  return {
    type: constants.LAYER_TOGGLE_VISIBLE,
    layerId,
  };
}

export function layerClose(layerId) {
  return {
    type: constants.LAYER_CLOSE,
    layerId,
  };
}

export function layerSetData(layerId, data, schema) {
  return {
    type: constants.LAYER_SET_DATA,
    layerId,
    data,
    schema,
  };
}

export function layerComputation(computation) {
  return {
    type: constants.LAYER_COMPUTATION_START,
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

export const geofenceToggleAlerts = () => ({
  type: constants.GEOFENCE_TOGGLE_ALERTS,
});

export function bufferLayer(layerId, bufferDistance) {
  return {
    type: constants.BUFFER_LAYER,
    layerId,
    bufferDistance,
  };
}

export const connectionUp = () => ({
  type: constants.CONNECTION_UP,
});

export const connectionDown = () => ({
  type: constants.CONNECTION_DOWN,
});

export const selectionSent = (seqNum) => ({
  type: constants.SELECTION_SENT,
  seqNum,
});

export const subscriptionsPost = (name, seqNum, data) => ({
  type: constants.SUBSCRIPTIONS_POST,
  name,
  seqNum,
  data,
});
