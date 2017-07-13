import * as React from "react";
import { IndexRedirect, Route, useRouterHistory } from "react-router";
import { createHashHistory } from "history";
import { App, Home } from "../containers";

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false });

function getRoutes() { return (
    <Route path="/" component={App}>
      <Route path="/datasets" component={Home} />
      <IndexRedirect to="/datasets" />
    </Route>
  );
}

export { getRoutes, appHistory };
