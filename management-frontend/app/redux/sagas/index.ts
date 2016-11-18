import { LOCATION_CHANGE } from "react-router-redux";
import { take, call, put, fork, cancel } from "redux-saga/effects";
import { takeLatest, takeEvery, SagaIterator } from "redux-saga";
declare var Notification: any;

import { fetchDatasets } from "../api";
import * as actions from "../actions";
import * as constants from "../constants";
//
// function prepare(generator): () => SagaIterator {
//   return function* () {
//     const forked = yield fork(generator);
//   };
// }

function* askForNotificationPermission() {
  yield call(Notification.requestPermission);
}

function watch(action, generator) {
  return function* () {
    yield* takeLatest(action, generator);
  };
}


function* watchLoadDatasets(): SagaIterator {
  while (true) {
    yield take(constants.FETCH_DATASETS);
    const res = yield call(fetchDatasets, "hello");

    if (! res.err) {
      yield put(actions.fetchDatasetsSuccess(res.data));
    }
  }
}

export function* sagas(): SagaIterator {
  yield fork(watchLoadDatasets);
}
