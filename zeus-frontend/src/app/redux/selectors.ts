import { selector } from "../api-management";
import * as api from "../api";

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

export const selectManaged = (state) => state.get("managed");
export const selectSessionInfo = selector(api.sessionInfo);
export const selectDatasetInfo = selector(api.datasetInfo);
export const selectDatasetContent = selector(api.datasetContent);
export const selectJobs = selector(api.jobs);
export const selectAssets = selector(api.assets);
export const selectAsset = selector(api.asset);
export const selectInsight = selector(api.insight);
