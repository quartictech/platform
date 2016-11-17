import { Set, Map, fromJS } from "immutable";
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

      return addEntry(deleteEntries(state), action.feature);  // Clear all entries, add this one

    case constants.LAYER_CLOSE:
      return state.deleteIn(["ids", action.layerId]);

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
      // Best course of action right now is just to deselect everything
      return initialState;

    default:
      return state;
  }
};

const initialState = fromJS({
  ids: {},
  entityIds: {},
  info: {
    lifecycleState: "INFO_NOT_REQUIRED",
    data: {},
  },
});

const addEntry = (state, feature) =>
  requireInfo(state)
  .updateIn(["ids", feature.layerId], new Set(), fids => fids.add(feature.id))
  .updateIn(["entityIds", feature.layerId], new Set(), fids => fids.add(feature.entityId));

const deleteEntries = (state) =>
  requireInfo(state)
  .set("ids", new Map())
  .set("entityIds", new Map());

const deleteEntry = (state, feature) =>
  requireInfo(state)
  .updateIn(["ids", feature.layerId], fids => fids.delete(feature.id))
  .update("ids", ids => (ids.get(feature.layerId).isEmpty() ? ids.delete(feature.layerId) : ids))
  .updateIn(["entityIds", feature.layerId], entityIds => entityIds.delete(feature.entityId))
  .update("entityIds", entityIds => (entityIds.get(feature.layerId).isEmpty() ? entityIds.delete(feature.layerId) : entityIds));

const requireInfo = (state) => setInfoLifecycleState(state, "INFO_REQUIRED");

const setInfoLifecycleState = (state, lifecycleState) => state
  .setIn(["info", "lifecycleState"], lifecycleState);
