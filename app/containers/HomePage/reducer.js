import { fromJS } from 'immutable';
import { IMPORT_LAYER, IMPORT_LAYER_DONE } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false,
});

function homeReducer(state = initialState, action) {
  switch (action.type) {
    case IMPORT_LAYER:
      console.log("loading");
      return state.set("loading", true);
    case IMPORT_LAYER_DONE:
      console.log("done loading " + action.layerId);
      console.log(state);
      return state.updateIn(["layers"], arr => arr.push(action.layerId));
    default:
      return state;
  }
}

export default homeReducer;
