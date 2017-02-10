import { LOCATION_CHANGE } from "react-router-redux";
import { datasetsReducer } from "./datasets";
import { uiReducer } from "./ui";
import { combineReducers } from "redux-immutable";
import { fromJS } from "immutable";

// Initial routing state
const routeInitialState = fromJS({
  locationBeforeTransitions: null,
});

/**
 * Merge route into the global application state
 */
function routeReducer(state = routeInitialState, action) {
  switch (action.type) {
    /* istanbul ignore next */
    case LOCATION_CHANGE:
      return state.merge({
        locationBeforeTransitions: action.payload,
      });
    default:
      return state;
  }
}

// TODO: Fix type!
const rootReducer: Redux.Reducer<any> = combineReducers({
  route: routeReducer,
  datasets: datasetsReducer,
  ui: uiReducer,
});

export { rootReducer }