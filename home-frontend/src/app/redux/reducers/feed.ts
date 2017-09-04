import { DatasetAction } from "../../models";
import * as constants from "../constants";
import { fromJS } from "immutable";

const initialState = fromJS({
  state: "not_loaded",
  items: [],
});

export function feedReducer(state = initialState, action: DatasetAction) {
  switch (action.type) {
    case constants.FETCH_FEED_SUCCESS:
      return fromJS({
        state: "loaded",
        items: action.data,
      });
    case constants.FETCH_FEED:
      return fromJS({
        state: "loading",
        items: [],
      });
    default:
      return state;
  }
}
