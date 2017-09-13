import { PipelineState } from "../redux/reducers/pipeline";

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

export const selectLoggedIn = state => state.getIn(["user", "loggedIn"]) as boolean;
export const selectDatasets = state => state.get("datasets").toJS();
export const selectPipeline = state => (state.get("pipeline").toJS()) as PipelineState;
export const selectNamespaces = state => Object.keys(state.get("datasets").toJS());
export const selectUi = state => state.get("ui").toJS();
