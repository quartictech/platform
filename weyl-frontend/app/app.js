/**
 * app.js
 *
 * This is the entry file for the application, only setup and boilerplate
 * code.
 */
import "babel-polyfill";

/* eslint-disable import/no-unresolved */
// Load the manifest.json file and the .htaccess file
import "!file?name=[name].[ext]!./manifest.json";
/* eslint-enable import/no-unresolved */

// Import all the third party stuff
import React from "react";
import ReactDOM from "react-dom";
import { Provider } from "react-redux";
import { applyRouterMiddleware, Router, createMemoryHistory } from "react-router";
import { syncHistoryWithStore } from "react-router-redux";
import useScroll from "react-router-scroll";
import configureStore from "./store";

// Import the CSS reset, which HtmlWebpackPlugin transfers to the build folder
// var $ = require("jquery");
window.$ = window.jQuery = require("jquery");
import "sanitize.css/sanitize.css";
import "semantic-ui/dist/semantic.css";
require("semantic-ui/dist/semantic");

// Create redux store with history
const underlyingHistory = createMemoryHistory();
const initialState = {};
const store = configureStore(initialState, underlyingHistory);

// Sync history and store, as the react-router-redux reducer
// is under the non-default key ("routing"), selectLocationState
// must be provided for resolving how to retrieve the "route" in the state
import { selectLocationState } from "containers/App/selectors";
const history = syncHistoryWithStore(underlyingHistory, store, {
  selectLocationState: selectLocationState(),
});

// Set up the router, wrapping all Routes in the App component
import App from "containers/App";
import createRoutes from "./routes";
const rootRoute = {
  component: App,
  childRoutes: createRoutes(store),
};

const style1 = "color: #dd137b";
const style2 = "color: #5e686e";
const style3 = `${style1}; font-weight: bold`;
console.log("%c\n" +  // eslint-disable-line no-console
  "                #######                \n" +
  "            /#  ##########*            \n" +
  "         #####  ##############         \n" +
  "     /########  #################,     \n" +
  "   (##########         ############*   \n" +
  "   #########*             /#########   \n" +
  "   #######       %c(##((%c       #######   \n" +
  "   #######    %c###((((((((%c    #######   \n" +
  "   #######    %c#(((((((((#%c    #######   \n" +
  "   #######    %c#(((((((((#%c    #######   \n" +
  "   #######    %c((((((((((((/%c    #####   \n" +
  "   #######       %c(((((    (#((%c    ,#   \n" +
  "   #########,                %c/(#(*%c     \n" +
  "   (##########         #####,    %c(((%c   \n" +
  "     /########  ###############(       \n" +
  "         #####  ##############,####.   \n" +
  "            /#  ##########*       /#   \n" +
  "                #######                \n" +
  "\n" +
  "          %chttps://quartic.io",
  style1, style2,
  style1, style2,
  style1, style2,
  style1, style2,
  style1, style2,
  style1, style2,
  style1, style2,
  style1, style2,
  style1, style3,
);


ReactDOM.render(
  <Provider store={store}>
    <Router
      history={history}
      routes={rootRoute}
      render={
        // Scroll to top when going to a new page, imitating default browser
        // behaviour
        applyRouterMiddleware(useScroll())
      }
    />
  </Provider>,
  document.getElementById("app")
);
