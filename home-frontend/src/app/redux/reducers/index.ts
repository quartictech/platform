import { LOCATION_CHANGE } from "react-router-redux";
import { datasetsReducer } from "./datasets";
import { pipelineReducer } from "./pipeline";
import { feedReducer } from "./feed";
import { userReducer } from "./user";
import { uiReducer } from "./ui";
import { combineReducers } from "redux-immutable";
import { fromJS } from "immutable";
import { client } from "../apollo";


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
  user: userReducer,
  datasets: datasetsReducer,
  pipeline: pipelineReducer,
  feed: feedReducer,
  ui: uiReducer,
  apollo: client.reducer(),
});

export { rootReducer };
