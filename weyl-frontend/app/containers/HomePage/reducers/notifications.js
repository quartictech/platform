import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.NOTIFICATIONS_UPDATE:
      return fromJS(action.notifications);
    default:
      return state;
  }
};
