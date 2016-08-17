

import { fromJS } from 'immutable';

const initialState = fromJS({
  layers: []
});

function homeReducer(state = initialState, action) {
  switch (action.type) {
    default:
      return state;
  }
}

export default homeReducer;
