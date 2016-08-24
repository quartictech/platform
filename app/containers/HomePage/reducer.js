import { fromJS } from 'immutable';
import { IMPORT_LAYER, IMPORT_LAYER_DONE } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false,
});

function homeReducer(state = initialState, action) {
  console.log("homeReducer");
  console.log(state.get("layers"));
  switch (action.type) {
    case IMPORT_LAYER:
      return state.set("loading", true);
    case IMPORT_LAYER_DONE:
      return state.updateIn(["layers"], arr => arr.push(action.layerId))
        .set("loading", false);
    default:
      return state;
  }
}

export default homeReducer;
