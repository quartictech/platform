import { fromJS } from 'immutable';
import {  SEARCH_DONE, ITEM_ADD } from './constants';

const initialState = fromJS({
  layers: [],
  loading: false,
});

function homeReducer(state = initialState, action) {
  switch (action.type) {
    case ITEM_ADD:
      return state.updateIn(["layers"], arr => arr.push(
        {id: action.id, name: action.name, description:action.description}));
    case SEARCH_DONE:
      action.callback(action.response);
    default:
      return state;
  }
}

export default homeReducer;
