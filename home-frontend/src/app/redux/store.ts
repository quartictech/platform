import { createStore, applyMiddleware } from "redux";
import { routerMiddleware } from "react-router-redux";
import { composeWithDevTools } from "redux-devtools-extension";
import { appHistory } from "../routes";
import { rootReducer } from "./reducers";

const router = routerMiddleware(appHistory);

import { sagas } from "./sagas";

import createSagaMiddleware from "redux-saga";
const sagaMiddleware = createSagaMiddleware();

import { client } from "./apollo";

/**
 * Creates a Redux Store from the given initialState
 */
 // TODO: fix type!
export function configureStore(initialState?: Object): Redux.Store<any> {
  const env: string = process.env.NODE_ENV;
  const middlewares: any[] = [client.middleware(), router, sagaMiddleware];

  /** Final Redux Store!!! */
  // TODO: fix type!
  const store: Redux.Store<any> = createStore(
    rootReducer,
    initialState,
    composeWithDevTools(
      applyMiddleware(...middlewares),
    ),
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
