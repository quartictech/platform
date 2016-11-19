import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  data: {},
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.CHART_SET_DATA:
      return state.set("data", action.data);
    default:
      return state;
  }
};
