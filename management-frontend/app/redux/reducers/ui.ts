import { UiAction } from "../../models";
import * as constants from "../constants";
import { Map } from "immutable";

const initialState = Map<string, any>();

export function uiReducer(state: Map<string, any> = initialState,
  action: UiAction) {
  switch (action.type) {
    case constants.UI_SET_ACTIVE_MODAL:
      return state.set("activeModal", action.activeModal);
    default:
      return state;
  }
}
