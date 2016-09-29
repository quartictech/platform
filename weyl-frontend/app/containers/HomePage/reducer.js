import { fromJS, Set } from "immutable";
import * as constants from "./constants";
import { themes } from "../../themes";

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
});

const colorScale = "PuRd";
const defaultLayerStyle = schema => ({
  type: "DEFAULT",
  property: schema.primaryAttribute,
  opacity: 0.8,
  point: {
    "circle-radius": 6,
    "color": "#e7298a", // "#223b53",
    colorScale,
  },
  polygon: {
    "color": "#67001f", // #F2F12D",
    "fill-outline-color": "#E0A025",
    colorScale,
  },
  line: {
    "color": "#e7298a",
    // color: "#E0A025",
    colorScale,
  },
});

const layerReducer = (layerState, action) => {
  switch (action.type) {
    case constants.LAYER_CREATE:
      return fromJS({
        id: action.id,
        name: action.name,
        description: action.description,
        visible: true,
        closed: false,
        style: defaultLayerStyle(action.attributeSchema),
        stats: action.stats,
        attributeSchema: action.attributeSchema,
        live: action.live,
        data: {},   // Only relevant in the case of live layers
        filter: {},
      });
    case constants.LAYER_TOGGLE_VISIBLE:
      return layerState.set("visible", !layerState.get("visible"));
    case constants.LAYER_CLOSE:
      return layerState.set("visible", false).set("closed", true);
    case constants.LAYER_SET_STYLE:
      return layerState.mergeIn(["style"], action.style);
    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
      return layerState.updateIn(["filter", action.attribute], new Set(), set => {
        if (set.has(action.value)) {
          return set.remove(action.value);
        }
        return set.add(action.value);
      });
    default:
      return layerState;
  }
};

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
  switch (action.type) {
    case constants.SEARCH_DONE:
      action.callback(action.response);
      return state;

    case constants.LAYER_CREATE:
      return state.updateIn(["layers"], arr => arr.push(layerReducer(undefined, action)));
    case constants.LAYER_TOGGLE_VISIBLE:
    case constants.LAYER_CLOSE:
    case constants.LAYER_SET_STYLE:
    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
      return state.updateIn(["layers"], arr => {
        const idx = arr.findKey(layer => layer.get("id") === action.layerId);
        const val = arr.get(idx);
        return arr.set(idx, layerReducer(val, action));
      });

    case constants.UI_TOGGLE: {
      const element = action.element;
      switch (element) {
        case "bucket":
        case "geofence":
          return state.updateIn(["ui", "layerOp"], val => ((val === element) ? null : element));
        case "theme":
          return state.updateIn(["ui", "settings", "theme"], val => themes[val].next);
        default:
          return state.updateIn(["ui", "panels", element], val => !val);
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
      return state.updateIn(["selection", "ids"], selection => selection.clear().merge(keyMap))
        .updateIn(["selection", "features"], features => features.clear().merge(action.features));
    }
    case constants.CLEAR_SELECTION:
      return state.setIn(["selection", "ids"], fromJS({}))
        .setIn(["selection", "features"], fromJS([]));

    case constants.NUMERIC_ATTRIBUTES_LOADED:
      return state.set("numericAttributes", fromJS(action.data));
    case constants.CHART_SELECT_ATTRIBUTE:
      return state.setIn(["histogramChart", "selectedAttribute"], action.attribute);

    case constants.MAP_LOADING:
    case constants.MAP_LOADED:
    case constants.MAP_MOUSE_MOVE:
      return state.update("map", mapState => mapReducer(mapState, action));

    case constants.GEOFENCE_EDIT_START:
    case constants.GEOFENCE_EDIT_FINISH:
    case constants.GEOFENCE_EDIT_CHANGE:
    case constants.GEOFENCE_SAVE_DONE:
    case constants.GEOFENCE_CHANGE_TYPE:
      return state.update("geofence", geofenceState => geofenceReducer(geofenceState, action));

    default:
      return state;
  }
}

export default homeReducer;
