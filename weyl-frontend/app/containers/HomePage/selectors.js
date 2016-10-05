
/**
 * Homepage selectors
 */

import { createSelector } from "reselect";

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

export const selectSelectionIds = () => createSelector(
  selectHome(),
  (homeState) => homeState.getIn(["selection", "ids"]).toJS()
);

export const selectSelectionFeatures = () => createSelector(
  selectHome(),
  selectLayers(),
  (home, layers) => {
    const features = home.getIn(["selection", "features"]).toJS();

    return Object.keys(features).map(k => {
      const layer = layers.find(l => l.id === features[k].layer.source);

      return {
        ...(features[k]),
        layer,
      };
    });
  }
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
