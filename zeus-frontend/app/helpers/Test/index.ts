/** Main Config File */
/** Redux Mock Store Configuration */
const configureStore = require("redux-mock-store");

import createSagaMiddleware from "redux-saga";

function createMockStore(initialState) {
  const sagaMiddleware = createSagaMiddleware();
  const middlewares = [sagaMiddleware];
  const mockStore = configureStore(middlewares);
  return mockStore(initialState);
}

export { createMockStore };
