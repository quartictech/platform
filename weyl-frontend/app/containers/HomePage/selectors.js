
/**
 * Homepage selectors
 */

import { createSelector } from "reselect";

const selectHome = () => (state) => state.get("home");

export const selectLayers = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("layers").toJS()
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
  (homeState) => {
    const features = homeState.getIn(["selection", "features"]).toJS();
    const layers = homeState.get("layers").toJS();

    return Object.keys(features).map(k => {
      const layer = layers.find(l => l.id === features[k].layer.source);
      const layerName = layer === undefined ? "" : layer.name;

      return {
        ...(features[k]),
        layerName,
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

export const selectNotifications = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("notifications").toJS(),
);

export const selectFeed = () => createSelector(
  selectHome(),
  (home) => home.get("feed").toJS(),
)

export const selectFeedPollInfo = () => createSelector(
  selectFeed(),
  selectLiveLayerIds(),
  (feed, layerIds) => ({ ...feed, layerIds }),
);
