import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  events: {},
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.FEED_SET_DATA:
      return state.setIn(["events", action.layerId], action.data);
    default:
      return state;
  }
};
