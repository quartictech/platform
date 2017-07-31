import * as React from "react";
import { IndexRedirect, Route, useRouterHistory } from "react-router";
import { createHashHistory } from "history";
import { App, DatasetsView, PipelineView, Login } from "../containers";
import { EnsureLoggedIn } from "./login";

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false });

function getRoutes() {
  return (
    <Route>
      <Route path="/login" component={Login} />
      <Route path="/" component={App}>
        <Route component={EnsureLoggedIn}>
          <Route path="/datasets" component={DatasetsView} />
          <Route path="/pipeline" component={PipelineView} />
          <IndexRedirect to="/datasets" />
        </Route>
      </Route>
    </Route>
  );
}

export { getRoutes, appHistory };
