import * as constants from "./constants";

export const layerListUpdate = (layers) => ({
  type: constants.LAYER_LIST_UPDATE,
  layers,
});

export const layerCreate = (layerId) => ({
  type: constants.LAYER_CREATE,
  layerId,
});

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

export function layerApplyTimeRangeFilter(layerId, attribute, startTime, endTime) {
  return {
    type: constants.LAYER_APPLY_TIME_RANGE_FILTER,
    layerId,
    attribute,
    startTime,
    endTime,
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

export const layerUpdate = (layerId, data, stats, dynamicSchema) => ({
  type: constants.LAYER_UPDATE,
  layerId,
  data,
  stats,
  dynamicSchema,
});

export function layerExport(layerId) {
  return {
    type: constants.LAYER_EXPORT,
    layerId,
  };
}


export function toggleUi(element) {
  return {
    type: constants.UI_TOGGLE,
    element,
  };
}

export const uiSetTheme = (theme) => ({
  type: constants.UI_SET_THEME,
  theme,
});

export function clearSelection() {
  return {
    type: constants.CLEAR_SELECTION,
  };
}

// Computation

export const computationStart = (computation) => ({
  type: constants.COMPUTATION_START,
  computation,
});

export const computationEnd = () => ({
  type: constants.COMPUTATION_END,
});

// Map

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

// Geofence

export const geofencePaneToggleVisibility = () => ({
  type: constants.GEOFENCE_PANE_TOGGLE_VISIBILITY,
});

export const geofenceCommitSettings = (settings) => ({
  type: constants.GEOFENCE_COMMIT_SETTINGS,
  settings,
});

export const geofenceSetManualControlsVisibility = (visible) => ({
  type: constants.GEOFENCE_SET_MANUAL_CONTROLS_VISIBILITY,
  visible,
});

export const geofenceSetManualGeometry = (geojson) => ({
  type: constants.GEOFENCE_SET_MANUAL_GEOMETRY,
  geojson,
});

export const geofenceSetGeometry = (geojson) => ({
  type: constants.GEOFENCE_SET_GEOMETRY,
  geojson,
});

export const geofenceSetViolations = (violations) => ({
  type: constants.GEOFENCE_SET_VIOLATIONS,
  violations,
});

export const geofenceToggleAlerts = () => ({
  type: constants.GEOFENCE_TOGGLE_ALERTS,
});

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
