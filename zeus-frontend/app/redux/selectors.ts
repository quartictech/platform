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

export const selectAssets = (state) => state.get("assets").toJS();
export const selectUi = (state) => state.get("ui").toJS();
