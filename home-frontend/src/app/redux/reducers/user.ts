import * as constants from "../constants";
import { fromJS } from "immutable";

const initialState = {
  loggedIn: false,
  profile: null,
};

export function userReducer(state = fromJS(initialState), action: any) {
  switch (action.type) {
    case constants.USER_LOGOUT:
      return state.set("loggedIn", false);
    case constants.USER_LOGIN_SUCCESS:
      return state.set("loggedIn", true);
    default:
      return state;
  }
}
