import { fromJS, Set } from 'immutable';
import {  SEARCH_DONE, LAYER_CREATE, LAYER_TOGGLE_VISIBLE, LAYER_CLOSE, UI_TOGGLE, SELECT_FEATURES, CLEAR_SELECTION, NUMERIC_ATTRIBUTES_LOADED, CHART_SELECT_ATTRIBUTE,
  LAYER_SET_STYLE, LAYER_TOGGLE_VALUE_VISIBLE
 } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false,
  ui: {
    layerOp: null,
    panels: {
      chart: false,
      layerList: true
    }
  },
  // Make this an object for now. Ugh.
  selection: {
    ids: {},
    features: []
  },
  numericAttributes: {},
  histogramChart: {
    selectedAttribute: null
  }
});

const defaultPolygonStyle = {
    "fill-color": "#006495", // #F2F12D",
    "fill-outline-color": "#E0A025", //#FF6600",
    "property": null,
    "fill-opacity": 0.8,
    "color-scale": null
  };

const defaultPointStyle = {
    'circle-radius': 1.75,
    'circle-color': '#223b53'
}

function defaultLayerStyle(schema) {
  let style = {
    polygon: Object.assign({}, defaultPolygonStyle),
    point: Object.assign({}, defaultPointStyle)
  };

  for (const attribute in schema.attributes) {
    if (schema.attributes[attribute].type == "NUMERIC") {
      style.polygon.property = attribute;
      style.polygon["color-scale"] = "BuPu"
      break;
    }
  }

  return style;
}

const layerReducer = (layerState, action) => {
  switch (action.type) {
    case LAYER_CREATE:
      return fromJS({
        id: action.id,
        name: action.name,
        description: action.description,
        visible: true,
        closed: false,
        style: defaultLayerStyle(action.attributeSchema),
        stats: action.stats,
        attributeSchema: action.attributeSchema,
        filter: {}
      });
    case LAYER_TOGGLE_VISIBLE:
      return layerState.set("visible", ! layerState.get("visible"));
    case LAYER_CLOSE:
      return layerState.set("visible", false).set("closed", true);
    case LAYER_SET_STYLE:
      return layerState.mergeIn(["style", "polygon"], action.style.polygon);
    case LAYER_TOGGLE_VALUE_VISIBLE:
      return layerState.updateIn(["filter", action.attribute], Set(), set => {
        if (set.has(action.value)) {
          return set.remove(action.value);
        } else {
          return set.add(action.value);
        }
      });
    default:
      return layerState;
  }
};

function homeReducer(state = initialState, action) {
  switch (action.type) {
    case SEARCH_DONE:
      action.callback(action.response);
      return state;

    case LAYER_CREATE:
      return state.updateIn(["layers"], arr => arr.push(layerReducer(undefined, action)));
    case LAYER_TOGGLE_VISIBLE:
    case LAYER_CLOSE:
    case LAYER_SET_STYLE:
    case LAYER_TOGGLE_VALUE_VISIBLE:
      return state.updateIn(["layers"], arr => {
        let idx = arr.findKey(layer => layer.get("id") === action.layerId);
        let val = arr.get(idx);
        return arr.set(idx, layerReducer(val, action));
      });

    case UI_TOGGLE:
      let element = action.element;
      if (element === "bucket") {
        return state.updateIn(["ui", "layerOp"], val => val == element ? null : element);
      }
      else {
        return state.updateIn(["ui", "panels", element], val => !val)
      }
    case SELECT_FEATURES:
    console.log("select features");
      let keyMap = {};
      for (var id of action.ids) {
        if (! keyMap.hasOwnProperty(id[0])) {
            keyMap[id[0]] = [];
        }
        keyMap[id[0]].push(id[1]);
      }
      console.log(action.features);
      return state.updateIn(["selection", "ids"], selection => selection.clear().merge(keyMap))
        .updateIn(["selection", "features"], features => features.clear().merge(action.features));
    case CLEAR_SELECTION:
      return state.setIn(["selection", "ids"], fromJS({}))
        .setIn(["selection", "features"], fromJS([]));
    case NUMERIC_ATTRIBUTES_LOADED:
      return state.set("numericAttributes", fromJS(action.data));
    case CHART_SELECT_ATTRIBUTE:
      return state.setIn(["histogramChart", "selectedAttribute"], action.attribute);

    default:
      return state;
  }
}

export default homeReducer;
