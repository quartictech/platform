import { createSelector } from "reselect";
const _ = require("underscore");

const selectHome = (state) => state.get("home").toJS();
const selectHomeImmutable = (state) => state.get("home"); // TODO: eventually everything will be immutable

export const selectLayerList = createSelector(selectHome, p => p.layerList);

function mergeStaticAndDynamicLayer(staticLayer, dynamicLayer) {
  if (dynamicLayer.getIn(["style", "attribute"]) == null && staticLayer.getIn(["staticSchema", "primaryAttribute"]) != null) {
    return staticLayer.merge(dynamicLayer).setIn(["style", "attribute"], staticLayer.getIn(["staticSchema", "primaryAttribute"]));
  }
  return staticLayer.merge(dynamicLayer);
}

export const selectLayers = createSelector(selectHomeImmutable, p => {
  const layerList = p.get("layerList");
  // join with the layer list
  return p.get("layers")
    .filter((layer, layerId) => layerList.has(layerId))
    .map((layer, layerId) => mergeStaticAndDynamicLayer(layerList.get(layerId), layer));
});

export const selectUi = createSelector(selectHome, p => p.ui);
export const selectSelection = createSelector(selectHome, p => p.selection);
export const selectGeofence = createSelector(selectHome, p => p.geofence);
export const selectConnectionUp = createSelector(selectHome, p => p.connection);
export const selectSubscriptions = createSelector(selectHomeImmutable, p => p.get("subscriptions"));

export const selectChart = createSelector(selectSubscriptions, p => p.get("chart"));
export const selectHistograms = createSelector(selectSubscriptions, p => p.get("histograms"));
export const selectAttributes = createSelector(selectSubscriptions, p => p.get("attributes"));

export const selectOpenLayerIds = createSelector(selectLayers,
  (layers) => _.values(layers.toJS())
    .map(layer => layer.id)
);

export const selectMap = createSelector(selectHome,
  (home) => ({
    ...(home.map),
    theme: home.ui.settings.theme,
  })
);
