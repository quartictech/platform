import "whatwg-fetch";
import * as React from "react";
import * as ReactDOM from "react-dom";
import { Provider } from "react-redux";
import { Router } from "react-router";
import { syncHistoryWithStore } from "react-router-redux";
import { configureStore } from "./redux/store";
import { getRoutes, appHistory } from "./routes";
import NoInternetExplorerView from "./containers/NoInternetExplorerView";

import "@blueprintjs/core/dist/blueprint.css";
import "@blueprintjs/table/dist/table.css";
import "plottable/plottable.css";
import "./plottable.css.global";

import { FocusStyleManager } from "@blueprintjs/core";
FocusStyleManager.onlyShowFocusOnTabs();      // To avoid annoying blue outlines

// TODO: fix type!
const store: Redux.Store<any> = configureStore();

import { selectLocationState } from "./redux/selectors";
const history = syncHistoryWithStore(appHistory, store, {
  selectLocationState: selectLocationState(),
});

// See https://stackoverflow.com/a/9851769/129570
const isInternetExplorer = () => !!(document as any).documentMode;

const component = isInternetExplorer()
  ? <NoInternetExplorerView />
  : <Router history={history}>{getRoutes()}</Router>;

ReactDOM.render(
  <Provider store={store}>
    {component}
  </Provider>,
  document.getElementById("app"),
);
