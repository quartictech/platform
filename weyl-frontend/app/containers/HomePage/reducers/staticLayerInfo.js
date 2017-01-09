import { fromJS } from "immutable";
import * as constants from "../constants";
import * as _ from "underscore";

// TODO: merge this sensibly with layers.js

export default (state = fromJS({}), action) => {
  switch (action.type) {
    case constants.LAYER_LIST_UPDATE:
      return fromJS(_.object(_.map(action.layers, item => [item.id, item])));
    default:
      return state;
  }
};
