import { Set, fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  ids: {},
  features: {},
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.MAP_MOUSE_CLICK:
      if (!action.feature) {
          return initialState;  // Clear everything
      }

      if (action.multiSelectEnabled) {
        return state.hasIn(["ids", action.feature.layerId, action.feature.id])
          ? deleteEntry(state, action.feature)
          : addEntry(state, action.feature);
      }

      return addEntry(initialState, action.feature);  // Clear all entries, add this one

    case constants.LAYER_CLOSE:
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

const addEntry = (state, feature) => state
  .updateIn(["ids", feature.layerId], new Set(), fids => fids.add(feature.id))
  .setIn(["features", feature.id], feature.properties);

const deleteEntry = (state, feature) => state
  .updateIn(["ids", feature.layerId], fids => fids.delete(feature.id))
  .deleteIn(["features", feature.id]);
