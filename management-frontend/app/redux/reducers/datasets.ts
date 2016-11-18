import { DatasetAction } from "../../models";
import * as constants from "../constants";
import { fromJS } from "immutable";

const initialState = fromJS({});

export function datasetsReducer(state = initialState, action: DatasetAction) {
  switch(action.type) {
    case constants.FETCH_DATASETS_SUCCESS:
      console.log("yay");
      return state;
    default:
      return state;
  }
}
