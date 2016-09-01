import { fromJS } from 'immutable';
import {  SEARCH_DONE, ITEM_ADD, LAYER_TOGGLE_VISIBLE } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false,
});

function homeReducer(state = initialState, action) {
  switch (action.type) {
    case ITEM_ADD:
      return state.updateIn(["layers"], arr => arr.push(
        fromJS({id: action.id, name: action.name, description:action.description, visible: true})));
    case SEARCH_DONE:
      action.callback(action.response);
      return state;
    case LAYER_TOGGLE_VISIBLE:
      console.log("Toggle layer");
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
