import { fromJS } from "immutable";
import * as constants from "../constants";

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.COMPUTATION_START:
      return state.set("active", true);
    case constants.COMPUTATION_END:
      return state.set("active", false);
    default:
      return state;
  }
};

const initialState = fromJS({
  active: false,
});
