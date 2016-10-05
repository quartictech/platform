
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

// https://gist.github.com/samgiles/762ee337dff48623e729
Array.prototype.flatMap = function (lambda) {  // eslint-disable-line no-extend-native
  return Array.prototype.concat.apply([], this.map(lambda));
};

export const selectSelectionFeatures = () => createSelector(
  selectHome(),
  selectLayers(),
  (home, layers) => {
    const ids = home.getIn(["selection", "ids"]).toJS();
    const features = home.getIn(["selection", "features"]).toJS();

    return Object.keys(ids)
      .flatMap(layerId => ids[layerId]
        .map(fid => ({
          layer: layers.find(l => l.id === layerId),
          properties: features[fid],
        }))
      );
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
