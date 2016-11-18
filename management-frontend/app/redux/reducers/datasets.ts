import { DatasetAction, IDataset } from "../../models";
import * as constants from "../constants";
import { Map, fromJS } from "immutable";

interface IState {
  datasets: { [id: string]: IDataset }
}

const initialState = fromJS({
  datasets: []
});

export function datasetsReducer(state: Map<string, any> = initialState, action: DatasetAction) {
  switch(action.type) {
    case constants.FETCH_DATASETS_SUCCESS:
      return state.set("datasets", action.data);
    default:
      return state;
  }
}
