import { PipelineAction } from "../../models";
import * as constants from "../constants";
import { fromJS } from "immutable";

export enum PipelineStatus {
  NOT_LOADED,
  LOADING,
  NOT_FOUND,
  LOADED,
}

export class PipelineState {
  data: any;
  status: PipelineStatus;
}

const initialState: PipelineState = {
  data: null,
  status: PipelineStatus.NOT_LOADED,
};

export function pipelineReducer(state: PipelineState = fromJS(initialState), action: PipelineAction) {
  switch (action.type) {
    case constants.FETCH_PIPELINE_SUCCESS:
      return fromJS({ status: PipelineStatus.LOADED, data: action.data });
    case constants.FETCH_PIPELINE:
      return fromJS({ status: PipelineStatus.LOADING, data: action.data });
    case constants.FETCH_PIPELINE_NOT_FOUND:
      return fromJS({ status: PipelineStatus.NOT_FOUND, data: {} });
    default:
      return state;
  }
}
