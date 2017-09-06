import { DatasetAction, LoadingState } from "../../models";
import * as constants from "../constants";
import { fromJS } from "immutable";

const initialState = fromJS({
  state: LoadingState,
  items: [],
});

export function feedReducer(state = initialState, action: DatasetAction) {
  switch (action.type) {
    case constants.FETCH_FEED_SUCCESS:
      return fromJS({
        state: LoadingState.LOADED,
        items: action.data,
      });
    case constants.FETCH_FEED:
      return fromJS({
        state: LoadingState.LOADING,
        items: [],
      });
    default:
      return state;
  }
}
