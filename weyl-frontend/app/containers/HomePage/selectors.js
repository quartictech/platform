
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

    return Object.keys(features).map(k => ({
      ...(features[k]),
      layerName: layers.find(l => l.id === features[k].layer.source).name
    }));
  }
);

const selectNumericAttributes = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("numericAttributes").toJS()
);

const selectHistogramChart = () => createSelector(
  selectHome(),
  (homeState) => homeState.get("histogramChart").toJS()
);

const selectMap = () => createSelector(
  selectHome(),
  (homeState) => {
    const style = homeState.getIn(["ui", "settings", "satellite"]) ? "satellite-streets" : "outdoors";
    return {
      ready: homeState.getIn(["map", "ready"]),
      style: `mapbox://styles/mapbox/${style}-v9`,
    };
  }
);

export {
  selectLayers,
  selectUi,
  selectSelectionIds,
  selectSelectionFeatures,
  selectNumericAttributes,
  selectHistogramChart,
  selectMap,
};
