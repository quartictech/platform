import { createSelector } from "reselect";
const _ = require("underscore");

const selectHome = () => (state) => state.get("home");

export const selectLayers = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("layers").toJS()
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

export const selectSelectionInfo = () => createSelector(
  selectSelection(),
  (selection) => selection.info
);

export const selectLiveLayerIds = () => createSelector(
  selectLayers(),
  (layers) => _.values(layers)
    .filter(layer => layer.live)
    .map(layer => layer.id)
);

export const selectTimeSeries = () => createSelector(
  selectSelection(),
  (selection) => selection.info.data.timeSeries,
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
