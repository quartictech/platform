import * as React from "react";
import { IndexRedirect, Route, useRouterHistory } from "react-router";
import { createHashHistory } from "history";
import App from "../containers/App";
import AssetView from "../containers/AssetView";
import BokehView from "../containers/BokehView";
import ExplorerView from "../containers/ExplorerView";
import InsightView from "../containers/InsightView";
import SearchView from "../containers/SearchView";

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false });

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/search" component={SearchView} />
      <Route path="/insights" component={InsightView} />
      <Route path="/bokeh" component={BokehView} />
      <Route path="/explorer/:datasetName" component={ExplorerView} />
      <Route path="/assets/:assetId" component={AssetView} />
      <Route path="/insights/:insightId" component={InsightView} />
      <IndexRedirect to="/search" />
    </Route>
  );
}

export { getRoutes, appHistory };
