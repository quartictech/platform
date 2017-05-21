import * as React from "react";
import { IndexRedirect, Route } from "react-router";
import { App, Insights, AssetView, ExplorerView, InsightView, SearchView} from "../containers";

import { useRouterHistory } from "react-router";
import { createHashHistory } from "history";

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false });

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/search" component={SearchView} />
      <Route path="/insights" component={Insights} />
      <Route path="/explorer/:datasetName" component={ExplorerView} />
      <Route path="/assets/:assetId" component={AssetView} />
      <Route path="/insights/:insightId" component={InsightView} />
      <IndexRedirect to="/search" />
    </Route>
  );
}

export { getRoutes, appHistory };
