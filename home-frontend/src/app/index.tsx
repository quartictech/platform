import "whatwg-fetch";
import * as React from "react";
import * as ReactDOM from "react-dom";
import { Router } from "react-router";
import { syncHistoryWithStore } from "react-router-redux";
import { configureStore } from "./redux/store";
import { getRoutes, appHistory } from "./routes";

import { ApolloProvider } from "react-apollo";
import { client } from "./redux/apollo";

import "@blueprintjs/core/dist/blueprint.css";

import { FocusStyleManager } from "@blueprintjs/core";
FocusStyleManager.onlyShowFocusOnTabs();      // To avoid annoying blue outlines

// TODO: fix type!
const store: Redux.Store<any> = configureStore();

import { selectLocationState } from "./redux/selectors";
const history = syncHistoryWithStore(appHistory, store, {
  selectLocationState: selectLocationState(),
});


const component = (
  <Router history={history}>
    {getRoutes()}
  </Router>
);

ReactDOM.render(
  <ApolloProvider client={client} store={store}>
    {component}
  </ApolloProvider>,
  document.getElementById("app"),
);
