import { fromJS, Set } from "immutable";
import * as constants from "../constants";
import { themes } from "../../../themes";
import layers from "./layers";

const initialState = fromJS({
  layers: [],
  ui: {
    layerOp: null,
    panels: {
      chart: false,
      layerList: true,
      settings: false,
    },
    settings: {
      theme: "dark",
    },
  },
  // Make this an object for now. Ugh.
  selection: {
    ids: {},
    features: [],
  },
  numericAttributes: {},
  map: {
    style: "basic",
    ready: false,
    mouseLocation: null, // Will be {lng,lat} when known
  },
  geofence: {
    editing: false,
    geojson: null,
    type: "INCLUDE",
  },
  notifications: {},
});

const mapReducer = (mapState, action) => {
  switch (action.type) {
    case constants.MAP_LOADING:
      return mapState.set("ready", false);
    case constants.MAP_LOADED:
      return mapState.set("ready", true);
    case constants.MAP_MOUSE_MOVE:
      return mapState.set("mouseLocation", action.mouseLocation);
    default:
      return mapState;
  }
};

const geofenceReducer = (geofenceState, action) => {
  switch (action.type) {
    case constants.GEOFENCE_EDIT_START:
      return geofenceState.set("editing", true);
    case constants.GEOFENCE_EDIT_CHANGE:
      return geofenceState.set("geojson", action.geojson);
    case constants.GEOFENCE_SAVE_DONE:
      return geofenceState.set("editing", false);
    case constants.GEOFENCE_CHANGE_TYPE:
      return geofenceState.set("type", action.value);
    default:
      return geofenceState;
  }
};

function homeReducer(state = initialState, action) {
  const state2 = state.update("layers", s => layers(s, action));

  switch (action.type) {
    case constants.SEARCH_DONE:
      action.callback(action.response);
      return state2;

    case constants.UI_TOGGLE: {
      const element = action.element;
      switch (element) {
        case "bucket":
        case "geofence":
          return state2.updateIn(["ui", "layerOp"], val => ((val === element) ? null : element));
        case "theme":
          return state2.updateIn(["ui", "settings", "theme"], val => themes[val].next);
        default:
          return state2.updateIn(["ui", "panels", element], val => !val);
      }
    }

    case constants.SELECT_FEATURES: {
      const keyMap = {};
      for (const id of action.ids) {
        if (!(id[0] in keyMap)) {
          keyMap[id[0]] = [];
        }
        keyMap[id[0]].push(id[1]);
      }
      return state2
        .updateIn(["selection", "ids"], selection => selection.clear().merge(keyMap))
        .updateIn(["selection", "features"], features => features.clear().merge(action.features));
    }
    case constants.CLEAR_SELECTION:
      return state2
        .setIn(["selection", "ids"], fromJS({}))
        .setIn(["selection", "features"], fromJS([]));

    case constants.NUMERIC_ATTRIBUTES_LOADED:
      return state2.set("numericAttributes", fromJS(action.data));
    case constants.CHART_SELECT_ATTRIBUTE:
      return state2.setIn(["histogramChart", "selectedAttribute"], action.attribute);

    case constants.MAP_LOADING:
    case constants.MAP_LOADED:
    case constants.MAP_MOUSE_MOVE:
      return state2.update("map", mapState => mapReducer(mapState, action));

    case constants.GEOFENCE_EDIT_START:
    case constants.GEOFENCE_EDIT_FINISH:
    case constants.GEOFENCE_EDIT_CHANGE:
    case constants.GEOFENCE_SAVE_DONE:
    case constants.GEOFENCE_CHANGE_TYPE:
      return state2.update("geofence", geofenceState => geofenceReducer(geofenceState, action));

    case constants.NOTIFICATIONS_UPDATE:
      return state2.set("notifications", fromJS(action.notifications));

    default:
      return state2;
  }
}

export default homeReducer;
