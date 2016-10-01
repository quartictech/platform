import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.NUMERIC_ATTRIBUTES_LOADED:
      return fromJS(action.data);
    default:
      return state;
  }
};
