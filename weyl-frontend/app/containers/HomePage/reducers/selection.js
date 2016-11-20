import { is, Set, Map, fromJS } from "immutable";
import * as constants from "../constants";

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.MAP_MOUSE_CLICK:
    case constants.LAYER_CLOSE:
    case constants.CLEAR_SELECTION:
      return incrementIfChanged(state, state.update("ids", ids => idsReducer(ids, action)));

    case constants.SELECTION_SENT:
      return state.set("latestSent", action.seqNum);

    default:
      return state;
  }
};

const incrementIfChanged = (state, nextState) => nextState.update("seqNum", i => (is(nextState, state) ? i : (i + 1)));

const idsReducer = (ids, action) => {
  switch (action.type) {
    case constants.MAP_MOUSE_CLICK:
      if (!action.feature) {
        return new Map();
      }

      if (action.multiSelectEnabled) {
        return ids.hasIn([action.feature.layerId, action.feature.entityId])
          ? deleteEntry(ids, action.feature)
          : addEntry(ids, action.feature);
      }

      return addEntry(new Map(), action.feature);  // Clear all entries, add this one

    case constants.LAYER_CLOSE:
      return ids.delete(action.layerId);

    case constants.CLEAR_SELECTION:
      return new Map();

    default:
      return ids;
  }
};

const addEntry = (ids, feature) => ids.update(feature.layerId, new Set(), eids => eids.add(feature.entityId));

const deleteEntry = (ids, feature) => {
  const updated = ids.update(feature.layerId, eids => eids.delete(feature.entityId));
  return updated.get(feature.layerId).isEmpty()
    ? updated.delete(feature.layerId)
    : updated;
};

const initialState = fromJS({
  ids: {},
  seqNum: 0,
  latestSent: 0,
});
