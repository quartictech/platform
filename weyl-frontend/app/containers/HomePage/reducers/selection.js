import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  ids: {},
  features: [],
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.SELECT_FEATURES: {
      const keyMap = {};
      for (const id of action.ids) {
        if (!(id[0] in keyMap)) {
          keyMap[id[0]] = [];
        }
        keyMap[id[0]].push(id[1]);
      }
      return state
        .update("ids", selection => selection.clear().merge(keyMap))
        .update("features", features => features.clear().merge(action.features));
    }
    case constants.CLEAR_SELECTION:
      return state
        .set("ids", fromJS({}))
        .set("features", fromJS([]));
    default:
      return state;
  }
};
