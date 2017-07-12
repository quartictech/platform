import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  style: "basic",
  ready: false,
  mouseLocation: null, // Will be {lng,lat} when known
  targetLocation: null, // Will be { center: [lng, lat], zoom: zoom } when known
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.MAP_LOADING:
      return state.set("ready", false);
    case constants.MAP_LOADED:
      return state.set("ready", true);
    case constants.MAP_MOUSE_MOVE:
      return state.set("mouseLocation", action.mouseLocation);
    case constants.MAP_SET_TARGET_LOCATION:
      return state.update("targetLocation", loc => loc || {
        center: [action.lng, action.lat],
        zoom: action.zoom,
      });
    default:
      return state;
  }
};
