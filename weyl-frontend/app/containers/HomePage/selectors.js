import { createSelector } from "reselect";
const _ = require("underscore");

const selectHome = (state) => state.get("home").toJS();
const selectHomeImmutable = (state) => state.get("home"); // TODO: eventually everything will be immutable

export const selectLayers = createSelector(selectHomeImmutable, p => p.get("layers"));
export const selectUi = createSelector(selectHome, p => p.ui);
export const selectSelection = createSelector(selectHome, p => p.selection);
export const selectGeofence = createSelector(selectHome, p => p.geofence);
export const selectConnectionUp = createSelector(selectHome, p => p.connection);
export const selectChart = createSelector(selectHome, p => p.chart);
export const selectHistogram = createSelector(selectHome, p => p.histogram);

export const selectSelectedIds = createSelector(selectSelection, p => p.ids);
export const selectSelectionInfo = createSelector(selectSelection, p => p.info);


export const selectLiveLayerIds = createSelector(selectLayers,
  (layers) => _.values(layers.toJS())
    .filter(layer => layer.live)
    .map(layer => layer.id)
);

export const selectMap = createSelector(selectHome,
  (home) => ({
    ...(home.map),
    theme: home.ui.settings.theme,
  })
);
