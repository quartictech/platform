import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.SEARCH_DONE:
      action.callback(action.response); // TODO: this is a side-effect so shouldn't live here
      return state;
    default:
      return state;
  }
};
