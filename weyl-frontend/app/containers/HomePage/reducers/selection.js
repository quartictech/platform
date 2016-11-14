import { Set, Map, fromJS } from "immutable";
import * as constants from "../constants";
const _ = require("underscore");

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.SELECTION_REMAP:
      return remapSelection(state, action);

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
      // TODO
      return state;

    default:
      return state;
  }
};

const initialState = fromJS({
  ids: {},
  externalIdToFeatureId: {},
  info: {
    lifecycleState: "INFO_NOT_REQUIRED",
    data: {},
  },
});

// TODO: This whole thing is a hack for the current id situation
const remapSelection = (state, action) => {
  let stuffChanged = false;
  const newState = state.updateIn(["ids", action.layerId],
    ids => {
      let s = ids;

      if (!s) return s;

      _.each(action.externalIdToFeatureId, (newId, extId) => {
        const currentId = state.getIn(["externalIdToFeatureId", action.layerId, extId]);
        if (s.has(currentId)) {
          s = s.delete(currentId).add(newId);
          stuffChanged = true;
        }
      });
      return s;
    }
  )
  .updateIn(["externalIdToFeatureId", action.layerId],
  externalIdToFeatureId => {
    let m = externalIdToFeatureId;
    if (!m) return m;
    _.each(action.externalIdToFeatureId,
      (newId, extId) => {
        if (m.has(extId)) {
          m = m.set(extId, newId);
          stuffChanged = true;
        }
      });
    return m;
  });

  if (stuffChanged) {
    return requireInfo(newState);
  }
  return newState;
};

const addEntry = (state, feature) =>
  requireInfo(state)
  .updateIn(["ids", feature.layerId], new Set(), fids => fids.add(feature.id))
  .updateIn(["externalIdToFeatureId", feature.layerId], new Map(),
    externalIds => (feature.externalId ? externalIds.set(feature.externalId, feature.id) : externalIds));

const deleteEntries = (state) =>
  requireInfo(state)
  .set("ids", new Map())
  .set("externalIdToFeatureId", new Map());

const deleteEntry = (state, feature) =>
  requireInfo(state)
  .updateIn(["ids", feature.layerId], fids => fids.delete(feature.id))
  .updateIn(["externalIdToFeatureId", feature.layerId],
    externalIds => (feature.externalId ? externalIds.delete(feature.externalId) : externalIds));

const requireInfo = (state) => setInfoLifecycleState(state, "INFO_REQUIRED");

const setInfoLifecycleState = (state, lifecycleState) => state
  .setIn(["info", "lifecycleState"], lifecycleState);
