import { fromJS } from "immutable";
import * as constants from "../constants";

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.GEOFENCE_COMMIT_SETTINGS:
      return state.set("settings", fromJS(action.settings));
    case constants.GEOFENCE_TOGGLE_MANUAL_CONTROLS_VISIBILITY:
      return state.set("manualControlsVisible", action.visible);
    case constants.GEOFENCE_SET_MANUAL_GEOMETRY:
      return state.set("manualGeojson", action.geojson || initialGeojson);
    case constants.GEOFENCE_SET_GEOMETRY:
      return state.set("geojson", action.geojson);
    case constants.GEOFENCE_SET_VIOLATED_GEOFENCES:
      return state.set("violatedIds", fromJS(action.violatedIds));
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

const initialState = fromJS({
  manualControlsVisible: false,
  manualGeojson: initialGeojson,

  settings: {
    mode: "layer",
    layerId: null,
    bufferDistance: 0,
  },

  geojson: initialGeojson,
  violatedIds: [],

  alertsEnabled: false,
});
