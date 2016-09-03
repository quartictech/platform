import { fromJS } from 'immutable';
import {  SEARCH_DONE, ITEM_ADD, LAYER_TOGGLE_VISIBLE } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false
});

const defaultPolygonStyle = {
    "fill-color": "#F2F12D",
    "property": null,
    "fill-opacity": 0.75
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
    default:
      return state;
  }
}

export default homeReducer;
