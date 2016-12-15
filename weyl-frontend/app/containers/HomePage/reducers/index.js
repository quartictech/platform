import { Map } from "immutable";
import layers from "./layers";
import layerList from "./layerList";
import map from "./map";
import geofence from "./geofence";
import ui from "./ui";
import selection from "./selection";
import subscriptions from "./subscriptions";
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
  layerList,
  map,
  geofence,
  ui,
  selection,
  subscriptions,
  connection,
});
