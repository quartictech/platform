import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS(true);

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.CONNECTION_UP:
      return fromJS(true);
    case constants.CONNECTION_DOWN:
      return fromJS(false);
    default:
      return state;
  }
};
