
/**
 * Homepage selectors
 */

import { createSelector } from "reselect";

const selectHome = () => (state) => state.get("home");

const selectLayers = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("layers").toJS()
);

const selectUi = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("ui").toJS()
);

const selectSelectionIds = () => createSelector(
  selectHome(),
  (homeState) => homeState.getIn(["selection", "ids"]).toJS()
);

const selectSelectionFeatures = () => createSelector(
  selectHome(),
  (homeState) => {
    const features = homeState.getIn(["selection", "features"]).toJS();
    const layers = homeState.get("layers").toJS();

    return Object.keys(features).map(k => {
      let layer = layers.find(l => l.id === features[k].layer.source);
      let layerName = layer === undefined ? "" : layer.name;

      return {
        ...(features[k]),
        layerName: layerName,
      }
    });
  }
);

const selectNumericAttributes = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("numericAttributes").toJS()
);

const selectMap = () => createSelector(
  selectHome(),
  (homeState) => {
    return {
      ...(homeState.get("map").toJS()),
      theme: homeState.getIn(["ui", "settings", "theme"]),
    };
  }
);

const selectGeofence = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("geofence").toJS(),
);

export {
  selectLayers,
  selectUi,
  selectSelectionIds,
  selectSelectionFeatures,
  selectNumericAttributes,
  selectMap,
  selectGeofence,
};
