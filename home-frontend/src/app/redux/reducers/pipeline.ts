import { PipelineAction } from "../../models";
import * as constants from "../constants";
import { fromJS } from "immutable";

export function pipelineReducer(state = fromJS({}), action: PipelineAction) {
  switch (action.type) {
    case constants.FETCH_PIPELINE_SUCCESS:
      return fromJS(action.data);
    default:
      return state;
  }
}
