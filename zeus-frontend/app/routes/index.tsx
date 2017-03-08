import * as React from "react";
import { IndexRedirect, Route } from "react-router";
import { App, Home } from "../containers";

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/datasets" component={Home} />
      <IndexRedirect to="/datasets" />
    </Route>
  );
};

export { getRoutes }
