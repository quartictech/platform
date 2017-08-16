import { UiAction, Ui } from "../../models";
import * as constants from "../constants";
import { Map, fromJS } from "immutable";

const initialState: Ui = {
  activeModal: null,
  searchString: null,
  namespace: null,
};

export function uiReducer(state: Map<string, any> = fromJS(initialState), action: UiAction) {
  switch (action.type) {
    case constants.UI_SET_ACTIVE_MODAL:
      return state.set("activeModal", action.activeModal);
    case constants.SEARCH_DATASETS:
      return state.set("searchString", action.search);
    case constants.SELECT_NAMESPACE:
      return state.set("namespace", action.namespace);
    default:
      return state;
  }
}
