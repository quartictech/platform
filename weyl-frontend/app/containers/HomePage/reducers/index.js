import { Map } from "immutable";
import layers from "./layers";
import map from "./map";
import geofence from "./geofence";
import ui from "./ui";
import selection from "./selection";
import numericAttributes from "./numericAttributes";
import histogramChart from "./histogramChart";
import search from "./search";
import notifications from "./notifications";
import feed from "./feed";

// Immutable version
const combineReducers = (reducers) =>
  (state = new Map(), action) =>
    Object.keys(reducers).reduce(
      (nextState, key) => nextState.update(key, v => reducers[key](v, action)),
      state
    );

export default combineReducers({
  layers,
  map,
  geofence,
  ui,
  selection,
  numericAttributes,
  histogramChart,
  search,
  notifications,
  feed,
});
