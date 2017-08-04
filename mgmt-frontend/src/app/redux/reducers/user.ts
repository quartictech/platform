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
    case constants.USER_FETCH_PROFILE_SUCCESS:
      return state.set("profile", fromJS(action.profile));
    default:
      return state;
  }
}
