import { DatasetAction } from "../../models";
import * as constants from "../constants";
import { List, fromJS } from "immutable";

const initialState = List<any>();

export function feedReducer(state: List<any> = initialState, action: DatasetAction) {
  switch (action.type) {
    case constants.FETCH_FEED_SUCCESS:
      return fromJS(action.data);
    default:
      return state;
  }
}
