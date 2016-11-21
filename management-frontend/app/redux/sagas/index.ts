import { take, call, put, fork } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import { fetchDatasets } from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

function* watchLoadDatasets(): SagaIterator {
  while (true) {
    yield take(constants.FETCH_DATASETS);
    const res = yield call(fetchDatasets);

    if (! res.err) {
      yield put(actions.fetchDatasetsSuccess(res.data));
    }
  }
}

export function* sagas(): SagaIterator {
  yield fork(watchLoadDatasets);
}
