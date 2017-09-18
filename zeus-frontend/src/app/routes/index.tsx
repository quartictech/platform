import * as React from "react";
import { IndexRedirect, Route, browserHistory } from "react-router";
import App from "../containers/App";
import AssetView from "../containers/AssetView";
import ExplorerView from "../containers/ExplorerView";
import InsightView from "../containers/InsightView";
import SearchView from "../containers/SearchView";

const appHistory = browserHistory;

function getRoutes() {
  return (
    <Route path="/" component={App}>
      <Route path="/search" component={SearchView} />
      <Route path="/insights/:insightName" component={InsightView} />
      <Route path="/explorer/:datasetName" component={ExplorerView} />
      <Route path="/assets/:assetId" component={AssetView} />
      <IndexRedirect to="/search" />
    </Route>
  );
}

export { getRoutes, appHistory };
