import { createSelector } from "reselect";
const _ = require("underscore");

const selectHome = (state) => state.get("home").toJS();

export const selectLayers = createSelector(selectHome, p => p.layers);
export const selectUi = createSelector(selectHome, p => p.ui);
export const selectSelection = createSelector(selectHome, p => p.selection);
export const selectGeofence = createSelector(selectHome, p => p.geofence);
export const selectFeed = createSelector(selectHome, p => p.feed);
export const selectConnectionUp = createSelector(selectHome, p => p.connection);

export const selectSelectionIds = createSelector(selectSelection, p => p.ids);
export const selectSelectionInfo = createSelector(selectSelection, p => p.info);
export const selectTimeSeries = createSelector(selectSelectionInfo, p => p.data.timeSeries);

export const selectLiveLayerIds = createSelector(selectLayers,
  (layers) => _.values(layers)
    .filter(layer => layer.live)
    .map(layer => layer.id)
);

export const selectMap = createSelector(selectHome,
  (home) => ({
    ...(home.map),
    theme: home.ui.settings.theme,
  })
);
