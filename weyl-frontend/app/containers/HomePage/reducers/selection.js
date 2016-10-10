import { Set, fromJS } from "immutable";
import * as constants from "../constants";

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

    case constants.LAYER_CLOSE: {
      const location = ["ids", action.layerId];
      if (state.hasIn(location)) {
        return state.getIn(location)
          .reduce(
            (prevState, fid) => prevState.deleteIn(["features", fid]),
            state
          )
          .deleteIn(location);
      }
      return state;
    }

    case constants.CLEAR_SELECTION:
      return initialState;

    case constants.SELECTION_INFO_LOADING:
      return setInfoLifecycleState(state, "INFO_LOADING");

    case constants.SELECTION_INFO_LOADED:
      // This check ensures we discard data returned by stale fetches
      if (state.getIn(["info", "lifecycleState"]) === "INFO_LOADING") {
        return setInfoLifecycleState(state, "INFO_LOADED")
          .setIn(["info", "data"], action.results);
      }
      return state;

    case constants.SELECTION_INFO_FAILED_TO_LOAD:
      // TODO
      return state;

    default:
      return state;
  }
};

const initialState = fromJS({
  ids: {},
  features: {},
  info: {
    lifecycleState: "INFO_NOT_REQUIRED",
    data: {},
  },
});

const addEntry = (state, feature) =>
  requireInfo(state)
  .updateIn(["ids", feature.layerId], new Set(), fids => fids.add(feature.id))
  .setIn(["features", feature.id], feature.properties);

const deleteEntry = (state, feature) =>
  requireInfo(state)
  .updateIn(["ids", feature.layerId], fids => fids.delete(feature.id))
  .deleteIn(["features", feature.id]);

const requireInfo = (state) => setInfoLifecycleState(state, "INFO_REQUIRED");

const setInfoLifecycleState = (state, lifecycleState) => state
  .setIn(["info", "lifecycleState"], lifecycleState);
