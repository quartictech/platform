import { DatasetAction } from "../../models";
import * as constants from "../constants";
import { Map, fromJS } from "immutable";

const initialState = Map<string, any>();

export function datasetsReducer(state: Map<string, any> = initialState,
  action: DatasetAction) {
  switch (action.type) {
    case constants.FETCH_DATASETS_SUCCESS:
      return fromJS(action.data);
    default:
      return state;
  }
}
