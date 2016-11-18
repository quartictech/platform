import { LOCATION_CHANGE } from "react-router-redux";
import { take, call, put, fork, cancel } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";
declare var Notification: any;

import { fetchDatasets } from "../api";
import * as actions from "../actions";

function prepare(generator): () => SagaIterator {
  return function* () {
    const forked = yield fork(generator);
  };
}

function* askForNotificationPermission() {
  yield call(Notification.requestPermission);
}

function* loadDatasets(): SagaIterator {
  const results = yield call(fetchDatasets);

  if (! results.err) {
    yield put(actions.fetchDatasetsSuccess());
  }
}

const sagas: (() => SagaIterator)[] = [
  prepare(askForNotificationPermission),
  prepare(loadDatasets)
];

export { sagas }
