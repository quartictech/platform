
// selectLocationState expects a plain JS object for the routing state
export const selectLocationState = () => {
  let prevRoutingState;
  let prevRoutingStateJS;

  return (state) => {
    const routingState = state.get("route"); // or state.route

    if (!routingState.equals(prevRoutingState)) {
      prevRoutingState = routingState;
      prevRoutingStateJS = routingState.toJS();
    }

    return prevRoutingStateJS;
  };
};

export const selectDatasets = (state) => state.get("datasetList").toJS();
export const selectAssets = (state) => state.get("assets").toJS();
export const selectAsset = (state) => state.get("asset").toJS();
export const selectNoobs = (state) => state.get("noobs").toJS();
export const selectInsights = (state) => state.get("insights").toJS();
export const selectUi = (state) => state.get("ui").toJS();
