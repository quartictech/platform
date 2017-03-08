import * as React from "react";
import { IndexRedirect, Route } from "react-router";
import { App, Insights, AssetView } from "../containers";

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/insights" component={Insights} />
      <Route path="/asset/:assetId" component={AssetView} />
      <IndexRedirect to="/insights" />
    </Route>
  );
};

export { getRoutes }
