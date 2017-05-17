import { createStore, applyMiddleware } from "redux";
import { routerMiddleware } from "react-router-redux";
import { browserHistory } from "react-router";
import { composeWithDevTools } from "redux-devtools-extension";
import { rootReducer } from "./reducers";
const logger = require("redux-logger")();
const router = routerMiddleware(browserHistory);

import { sagas } from "./sagas";

import createSagaMiddleware from "redux-saga";
const sagaMiddleware = createSagaMiddleware();

/**
 * Creates a Redux Store from the given initialState
 */
 // TODO: fix type!
export function configureStore(initialState?: Object): Redux.Store<any> {
  const env: string = process.env.NODE_ENV;

  let middlewares: any[] = [router, sagaMiddleware];

  if (env === "development") {
    middlewares.push(logger);
  }

  /** Final Redux Store!!! */
  // TODO: fix type!
  const store: Redux.Store<any> = createStore(
    rootReducer,
    initialState,
    composeWithDevTools(
      applyMiddleware(...middlewares)
    )
  );

  /** Adds Hot Reloading Capability to Reducers in Dev. Mode */
  if (env === "development" && (module as any).hot) {
    (module as any).hot.accept("./reducers", () => {
      store.replaceReducer((require("./reducers")));
    });
  }

  sagaMiddleware.run(sagas);

  return store;

}
