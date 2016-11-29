import { take, call, put, fork } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import { fetchDatasets, uploadFile, createDataset } from "../api";
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

function* watchCreateDataset(): SagaIterator {
  while (true) {
    const action = yield take(constants.CREATE_DATASET);
    const res = yield call(uploadFile, action.data.files.files);

    if (!res.err) {
      yield call(createDataset, action.data.metadata, res.data, action.data.files.fileType);
      yield put(actions.setActiveModal(null));
      yield put(actions.fetchDatasets());
    }
  }
}

export function* sagas(): SagaIterator {
  yield fork(watchLoadDatasets);
  yield fork(watchCreateDataset);
}
