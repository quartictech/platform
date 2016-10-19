import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  editing: false,
  geojson: {
    type: "FeatureCollection",
    features: [],
  },
  bufferDistance: 0,
  layerId: null,
  type: "INCLUDE",
  violatedIds: [],
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.GEOFENCE_EDIT_START:
      return state.set("editing", true);
    case constants.GEOFENCE_SET_GEOMETRY:
      return state.set("geojson", action.geojson);
    case constants.GEOFENCE_SAVE_DONE:
      return state.set("layerId", null)
        .set("editing", false);
    case constants.GEOFENCE_CHANGE_TYPE:
      return state.set("type", action.value);
    case constants.GEOFENCE_SET_LAYER:
      return state.set("layerId", action.layerId)
        .set("bufferDistance", action.bufferDistance);
    case constants.GEOFENCE_SET_VIOLATED_GEOFENCES:
      return state.set("violatedIds", fromJS(action.violatedIds));
    default:
      return state;
  }
};
