import { createStore, applyMiddleware } from "redux";
import { routerMiddleware } from "react-router-redux";
import { appHistory } from "../routes";
import { composeWithDevTools } from "redux-devtools-extension";
import { createLogger } from "redux-logger";
import { rootReducer } from "./reducers";

const router = routerMiddleware(appHistory);

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
    middlewares.push(createLogger({
      collapsed: true,
    }));
  }

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
