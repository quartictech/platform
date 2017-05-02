import { createSelector } from "reselect";
const _ = require("underscore");

const selectHome = (state) => state.get("home").toJS();
const selectHomeImmutable = (state) => state.get("home"); // TODO: eventually everything will be immutable

export const selectLayerList = createSelector(selectHome, p => p.staticLayerInfo);

// HACK: The static info contains the primaryAttribute from the staticSchema but
// the dynamicInfo must contain the style (and the attribute used to colour things which
// may be overriden by the user). This function joins the two parts together accounting for this.
function mergeStaticAndDynamicLayer(staticInfo, dynamicInfo) {
  if (dynamicInfo.getIn(["style", "attribute"]) == null && staticInfo.getIn(["staticSchema", "primaryAttribute"]) != null) {
    return staticInfo.merge(dynamicInfo).setIn(["style", "attribute"], staticInfo.getIn(["staticSchema", "primaryAttribute"]));
  }
  return staticInfo.merge(dynamicInfo);
}

export const selectLayers = createSelector(selectHomeImmutable, p => {
  const staticLayerInfo = p.get("staticLayerInfo");
  // join with the layer list and filter layers for which we don't yet have static info
  return p.get("dynamicLayerInfo")
    .filter((layer, layerId) => staticLayerInfo.has(layerId))
    .map((layer, layerId) => mergeStaticAndDynamicLayer(staticLayerInfo.get(layerId), layer));
});

export const selectUi = createSelector(selectHome, p => p.ui);
export const selectSelection = createSelector(selectHome, p => p.selection);
export const selectComputation = createSelector(selectHome, p => p.computation);
export const selectGeofence = createSelector(selectHome, p => p.geofence);
export const selectConnectionUp = createSelector(selectHome, p => p.connection);
export const selectSubscriptions = createSelector(selectHomeImmutable, p => p.get("subscriptions"));

export const selectChart = createSelector(selectSubscriptions, p => p.get("chart"));
export const selectDetails = createSelector(selectSubscriptions, p => p.get("details"));
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
