import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  style: "basic",
  ready: false,
  mouseLocation: null, // Will be {lng,lat} when known
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.MAP_LOADING:
      return state.set("ready", false);
    case constants.MAP_LOADED:
      return state.set("ready", true);
    case constants.MAP_MOUSE_MOVE:
      return state.set("mouseLocation", action.mouseLocation);
    default:
      return state;
  }
};
