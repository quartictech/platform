import { fromJS } from "immutable";
import * as constants from "../constants";

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.GEOFENCE_EDIT_START:
      return state.set("editing", true);
    case constants.GEOFENCE_EDIT_FINISH:
      return state.set("editing", false);
    case constants.GEOFENCE_EDIT_SET_GEOMETRY:
      return state
        .set("editGeojson", action.geojson)
        .set("layerId", null)
        .set("bufferDistance", 0);
    case constants.GEOFENCE_EDIT_SET_LAYER:
      return state
        .set("editGeojson", initialGeojson)
        .set("layerId", action.layerId)
        .set("bufferDistance", action.bufferDistance);
    case constants.GEOFENCE_EDIT_SET_TYPE:
      return state.set("type", action.value);
    case constants.GEOFENCE_SET_GEOMETRY:
      return state.set("geojson", action.geojson);
    case constants.GEOFENCE_SET_BUFFER_DISTANCE:
      return state.set("bufferDistance", action.bufferDistance);
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
  editing: false,
  editGeojson: initialGeojson,
  geojson: initialGeojson,
  bufferDistance: 0,
  layerId: null,
  type: "EXCLUDE",
  violatedIds: [],
  alertsEnabled: false,
});
