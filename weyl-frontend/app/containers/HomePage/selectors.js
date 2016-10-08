import { createSelector } from "reselect";
const _ = require("underscore");

const selectHome = () => (state) => state.get("home");

export const selectLayers = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("layers").toJS()
);

export const selectLayer = (layerId) => createSelector(
  selectLayers(),
  (layers) => layers.find(l => l.id === layerId)
);

export const selectUi = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("ui").toJS()
);

export const selectSelection = () => createSelector(
  selectHome(),
  (home) => home.get("selection").toJS()
);

export const selectSelectionIds = () => createSelector(
  selectSelection(),
  (selection) => selection.ids
);

export const selectSelectionFeatures = () => createSelector(
  selectSelection(),
  selectLayers(),
  (selection, layers) => _.chain(selection.ids).keys()
    .map(layerId => selection.ids[layerId]
      .map(fid => ({
        layer: layers.find(l => l.id === layerId),
        properties: selection.features[fid],
      }))
    )
    .flatten()
    .value()
);

export const selectAggregatesLifecycleState = () => createSelector(
  selectSelection(),
  (selection) => selection.aggregates.lifecycleState
);

export const selectLiveLayerIds = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("layers").toJS()
    .filter(layer => layer.live)
    .map(layer => layer.id)
);

export const selectNumericAttributes = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("numericAttributes").toJS()
);

export const selectMap = () => createSelector(
  selectHome(),
  (homeState) => ({
    ...(homeState.get("map").toJS()),
    theme: homeState.getIn(["ui", "settings", "theme"]),
  })
);

export const selectGeofence = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("geofence").toJS(),
);

export const selectFeed = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("feed").toJS(),
);

export const selectConnectionUp = () => createSelector(
  selectHome(),
  (home) => home.get("connection"),
);
