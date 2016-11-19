import { fromJS } from "immutable";
import * as constants from "../constants";

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.SUBSCRIPTIONS_POST:
      return state.set(action.name, fromJS({ seqNum: action.seqNum, data: action.data }));
    default:
      return state;
  }
};

const initialState = fromJS({
  attributes: { seqNum: 0, data: {} },
  histograms: { seqNum: 0, data: {} },
  chart: { seqNum: 0, data: {} },
});
