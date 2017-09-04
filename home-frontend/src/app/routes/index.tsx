import * as React from "react";
import { IndexRoute, Route, useRouterHistory } from "react-router";
import { createHashHistory } from "history";
import { App, DatasetsView, PipelineView, Login, HomeView } from "../containers";
import { EnsureLoggedIn } from "./login";

const appHistory = useRouterHistory(createHashHistory)({ queryKey: false });

function getRoutes() {
  return (
    <Route>
      <Route path="/login" component={Login} />
      <Route path="/" component={App}>
        <Route component={EnsureLoggedIn}>
          <IndexRoute component={HomeView} />
          <Route path="/datasets" component={DatasetsView} />
          <Route path="/pipeline(/:build)" component={PipelineView} />
        </Route>
      </Route>
    </Route>
  );
}

export { getRoutes, appHistory };
