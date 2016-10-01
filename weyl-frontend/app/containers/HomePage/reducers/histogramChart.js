import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  selectedAttribute: null,
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.CHART_SELECT_ATTRIBUTE:
      return state.set("selectedAttribute", action.attribute);
    default:
      return state;
  }
};
