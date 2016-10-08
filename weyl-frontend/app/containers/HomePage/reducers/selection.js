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

    case constants.AGGREGATES_LOADING:
      return setAggregatesLifecycleState(state, "AGGREGATES_LOADING");

    case constants.AGGREGATES_LOADED:
      // This check ensures we discard data returned by stale fetches
      if (state.getIn(["aggregates", "lifecycleState"]) === "AGGREGATES_LOADING") {
        return setAggregatesLifecycleState(state, "AGGREGATES_LOADED")
          .setIn(["aggregates", "data"], action.results);
      }
      return state;

    case constants.AGGREGATES_FAILED_TO_LOAD:
      // TODO

    default:
      return state;
  }
};

const initialState = fromJS({
  ids: {},
  features: {},
  aggregates: {
    lifecycleState: "AGGREGATES_NOT_REQUIRED",
    data: {},
  },
});

const addEntry = (state, feature) =>
  requireAggregates(state)
  .updateIn(["ids", feature.layerId], new Set(), fids => fids.add(feature.id))
  .setIn(["features", feature.id], feature.properties);

const deleteEntry = (state, feature) =>
  requireAggregates(state)
  .updateIn(["ids", feature.layerId], fids => fids.delete(feature.id))
  .deleteIn(["features", feature.id]);

const requireAggregates = (state) => setAggregatesLifecycleState(state, "AGGREGATES_REQUIRED");

const setAggregatesLifecycleState = (state, lifecycleState) => state
  .setIn(["aggregates", "lifecycleState"], lifecycleState);
