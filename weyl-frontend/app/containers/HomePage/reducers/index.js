import { Map } from "immutable";
import layers from "./layers";
import map from "./map";
import geofence from "./geofence";
import ui from "./ui";
import selection from "./selection";
import chart from "./chart";
import histogram from "./histogram";
import attributes from "./attributes";
import connection from "./connection";

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
  chart,
  histogram,
  attributes,
  connection,
});
