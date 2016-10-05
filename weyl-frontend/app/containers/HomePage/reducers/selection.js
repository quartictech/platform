import { Set, fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  ids: {},
  features: {},
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.MAP_CLICK_FEATURE:
      if (state.hasIn(["ids", action.layerId, action.featureId])) {
        // Delete entry
        return state
          .updateIn(["ids", action.layerId], fids => fids.delete(action.featureId))
          .deleteIn(["features", action.featureId]);
      }

      if (action.ctrlPressed) {
        // Add entry
        return state
          .updateIn(["ids", action.layerId], new Set(), fids => fids.add(action.featureId))
          .setIn(["features", action.featureId], action.featureProperties);
      }

      // Clear all entries, add this one
      return initialState
        .setIn(["ids", action.layerId], new Set([action.featureId]))
        .setIn(["features", action.featureId], action.featureProperties);

    case constants.LAYER_CLOSE:
      console.log("layerId", action.layerId);
      return state.getIn(["ids", action.layerId])
        .reduce(
          (prevState, fid) => prevState.deleteIn(["features", fid]),
          state
        )
        .deleteIn(["ids", action.layerId]);

    case constants.CLEAR_SELECTION:
      return initialState;

    default:
      return state;
  }
};
