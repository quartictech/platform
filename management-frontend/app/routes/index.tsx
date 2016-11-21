import * as React from "react";
import { IndexRedirect, Route } from "react-router";
import { App, Home, DatasetView } from "../containers";

function getRoutes(store) { return (
    <Route path="/" component={App}>
      <Route path="/datasets" component={Home} />
      <IndexRedirect to="/datasets" />
      <Route path="/dataset/:id" component={DatasetView} />
    </Route>
  );
};

export { getRoutes }
