import { Set, fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  ids: {},
  features: [],
  multiSelectEnabled: false,
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.MAP_CLICK_FEATURE: {
      const alreadyContains = state.hasIn(["ids", action.layerId, action.featureId]);
      const reducer = alreadyContains
        ? (fids => fids.delete(action.featureId))
        : action.ctrlPressed
          ? (fids => fids.add(action.featureId))
          : (fids => new Set([action.featureId]));
      return state
        .updateIn(["ids", action.layerId], new Set(), reducer)
        .set("features", fromJS(action.feature));
    }

    case constants.CLEAR_SELECTION:
      return state
        .set("ids", fromJS({}))
        .set("features", fromJS([]));

    default:
      return state;
  }
};
