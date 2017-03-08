import * as React from "react";
import { IndexRedirect, Route } from "react-router";
import { App, Home, AssetView } from "../containers";

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/datasets" component={Home} />
      <Route path="/asset/:assetId" component={AssetView} />
      <IndexRedirect to="/datasets" />
    </Route>
  );
};

export { getRoutes }
