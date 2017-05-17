import * as React from "react";
import { IndexRedirect, Route } from "react-router";
import { App, Insights, AssetView, Inventory, InsightView } from "../containers";

import { useRouterHistory } from 'react-router'
import { createHashHistory } from 'history'

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false })

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/insights" component={Insights} />
      <Route path="/inventory" component={Inventory} />
      <Route path="/assets/:assetId" component={AssetView} />
      <Route path="/insights/:insightId" component={InsightView} />
      <IndexRedirect to="/insights" />
    </Route>
  );
}

export { getRoutes, appHistory };
