import "whatwg-fetch";
import * as React from "react";
import * as ReactDOM from "react-dom";
import { Provider } from "react-redux";
import { Router } from "react-router";
import { syncHistoryWithStore } from "react-router-redux";
import { configureStore } from "./redux/store";
import { getRoutes, appHistory } from "./routes";

import "@blueprintjs/core/dist/blueprint.css";

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
  <Provider store={store}>
    {component}
  </Provider>,
  document.getElementById("app"),
);
