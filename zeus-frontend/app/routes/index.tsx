import * as React from "react";
import { IndexRedirect, Route, useRouterHistory } from "react-router";
import { createHashHistory } from "history";
import { App, AssetView, ExplorerView, InsightView, SearchView, SchematicView } from "../containers";

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false });

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/search" component={SearchView} />
      <Route path="/insights" component={InsightView} />
      <Route path="/explorer/:datasetName" component={ExplorerView} />
      <Route path="/assets/:assetId" component={AssetView} />
      <Route path="/insights/:insightId" component={InsightView} />
      <Route path="/schematic" component={SchematicView} />
      <IndexRedirect to="/search" />
    </Route>
  );
}

export { getRoutes, appHistory };
