import { fromJS } from "immutable";
import * as constants from "../constants";

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.GEOFENCE_PANE_TOGGLE_VISIBILITY:
      return state.update("paneVisible", x => !x);
    case constants.GEOFENCE_COMMIT_SETTINGS:
      return state.set("settings", fromJS(action.settings));
    case constants.GEOFENCE_SET_MANUAL_CONTROLS_VISIBILITY:
      return state.set("manualControlsVisible", action.visible);
    case constants.GEOFENCE_SET_MANUAL_GEOMETRY:
      return state.set("manualGeojson", fromJS(action.geojson) || initialGeojson);
    case constants.GEOFENCE_SET_GEOMETRY:
      return state.set("geojson", action.geojson);
    case constants.GEOFENCE_SET_VIOLATIONS:
      return state.set("violations", fromJS(action.violations));
    case constants.GEOFENCE_TOGGLE_ALERTS:
      return state.update("alertsEnabled", x => !x);
    default:
      return state;
  }
};

const initialGeojson = fromJS({
  type: "FeatureCollection",
  features: [],
});

const initialSettings = fromJS({
  mode: "layer",
  layerId: null,
  bufferDistance: 0,
  defaultLevel: "INFO",
});

const initialState = fromJS({
  paneVisible: false,
  manualControlsVisible: false,
  manualGeojson: initialGeojson,
  settings: initialSettings,
  geojson: initialGeojson,
  violations: {
    ids: [],
    numInfo: 0,
    numWarning: 0,
    numSevere: 0,
  },
  alertsEnabled: false,
});
