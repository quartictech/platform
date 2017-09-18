import * as React from "react";
import { IndexRoute, Route, browserHistory } from "react-router";
import {
  App,
  DatasetsView,
  DatasetView,
  PipelineView,
  Login,
  HomeView,
  BuildView,
} from "../containers";
import { EnsureLoggedIn } from "./login";

const appHistory = browserHistory;

function getRoutes() {
  return (
    <Route>
      <Route path="/login" component={Login} />
      <Route path="/" component={App}>
        <Route component={EnsureLoggedIn}>
          <IndexRoute component={HomeView} />
          <Route path="/datasets" component={DatasetsView} />
          <Route path="/datasets/:namespace/:id" component={DatasetView} />
          <Route path="/pipeline(/:build)" component={PipelineView} />
          <Route path="/build/:build" component={BuildView} />
        </Route>
      </Route>
    </Route>
  );
}

export { getRoutes, appHistory };
