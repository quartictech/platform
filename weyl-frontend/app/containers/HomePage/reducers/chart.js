import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  timeseries: {},
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.CHART_SET_TIMESERIES:
      return state.set("timeseries", action.timeseries);
    default:
      return state;
  }
};
