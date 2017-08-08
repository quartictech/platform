import { call, put, select, takeEvery } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";
import { Intent } from "@blueprintjs/core";

import { toaster } from "../../containers/App/toaster";

import * as actions from "../actions";
import * as selectors from "../selectors";

export function watch(action, generator) {
  return function* () {
    yield takeEvery(action, generator);
  };
}

export function* checkedApiCall(apiFunction, ...args): SagaIterator {
  const res = yield call(apiFunction, ...args);
  const loggedIn = yield select(selectors.selectLoggedIn);
  if (loggedIn && res.err && res.err.status === 401) {
    yield put(actions.userLogout());
  }
  return res;
}

export function showSuccess(message) {
  toaster.show({
    intent: Intent.SUCCESS,
    message,
  });
}
