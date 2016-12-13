import { fromJS, Map } from "immutable";
import * as constants from "../constants";

// TODO: merge this sensibly with layers.js

export default (state = new Map(), action) => {
  switch (action.type) {
    case constants.LAYER_LIST_UPDATE:
      return fromJS(action.layers);
    default:
      return state;
  }
};
