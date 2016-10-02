import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  events: [],
});

export default (state = initialState, action) => {
  switch (action.type) {
    default:
      return state;
  }
};
