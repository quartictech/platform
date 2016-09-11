import { fromJS } from 'immutable';
import {  SEARCH_DONE, ITEM_ADD, LAYER_TOGGLE_VISIBLE, UI_TOGGLE, SELECT_FEATURES, CLEAR_SELECTION, NUMERIC_ATTRIBUTES_LOADED, CHART_SELECT_ATTRIBUTE } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false,
  ui: {
    layerOp: null,
    panels: {
      chart: false
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
    "fill-color": "#F2F12D",
    "property": null,
    "fill-opacity": 0.8,
    "color-scale": null
  };

const defaultPointStyle = {
    'circle-radius': 1.75,
    'circle-color': '#223b53'
}

function defaultLayerStyle(stats) {
  let style = {
    polygon: Object.assign({}, defaultPolygonStyle),
    point: Object.assign({}, defaultPointStyle)
  };

  for (const attribute in stats.attributeStats) {
    console.log(attribute);
    if (stats.attributeStats[attribute].type == "NUMERIC") {
      style.polygon.property = attribute;
      break;
    }
  }

  return style;
}

function homeReducer(state = initialState, action) {
  switch (action.type) {
    case ITEM_ADD:
      return state.updateIn(["layers"], arr => arr.push(
        fromJS({
          id: action.id,
          name: action.name,
          description: action.description,
          visible: true,
          style: defaultLayerStyle(action.stats),
          stats: action.stats
        })));
    case SEARCH_DONE:
      action.callback(action.response);
      return state;
    case LAYER_TOGGLE_VISIBLE:
      return state.updateIn(["layers"], arr => {
        let idx = arr.findKey(layer => layer.get("id") === action.id);
        let val = arr.get(idx);
        return arr.set(idx, val.set("visible", ! val.get("visible")));
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
    console.log("YEH");
      return state.setIn(["histogramChart", "selectedAttribute"], action.attribute);
    default:
      return state;
  }
}

export default homeReducer;
