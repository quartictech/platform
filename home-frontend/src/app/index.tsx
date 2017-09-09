import "whatwg-fetch";
import * as React from "react";
import * as ReactDOM from "react-dom";
import { Router } from "react-router";
import { syncHistoryWithStore } from "react-router-redux";
import { configureStore } from "./redux/store";
import { getRoutes, appHistory } from "./routes";

import { apiRootUrl } from "./redux/api";

import { ApolloClient, createNetworkInterface, ApolloProvider } from "react-apollo";

import { QUARTIC_XSRF, QUARTIC_XSRF_HEADER } from "./helpers/Utils";

import "@blueprintjs/core/dist/blueprint.css";

import { FocusStyleManager } from "@blueprintjs/core";
FocusStyleManager.onlyShowFocusOnTabs();      // To avoid annoying blue outlines

// TODO: fix type!
const store: Redux.Store<any> = configureStore();

import { selectLocationState } from "./redux/selectors";
const history = syncHistoryWithStore(appHistory, store, {
  selectLocationState: selectLocationState(),
});

const networkInterface = createNetworkInterface({
  uri: `${apiRootUrl}/graphql/execute`,
  opts: {
    credentials: "same-origin",
  },
});

networkInterface.use([{
  applyMiddleware(req, next) {
    if (!req.options.headers) {
      req.options.headers = {};  // Create the header object if needed.
    }
    // get the authentication token from local storage if it exists
    const token = localStorage.getItem(QUARTIC_XSRF);
    req.options.headers[QUARTIC_XSRF_HEADER] = token;
    next();
  },
}]);

const client = new ApolloClient({ networkInterface });

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
