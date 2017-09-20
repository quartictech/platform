import { call, fork, put } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { checkedApiCall, watch, showSuccess } from "./utils";

function* fetchDatasets(_action): SagaIterator {
  const res = yield* checkedApiCall(api.fetchDatasets);
  if (!res.err) {
    yield put(actions.fetchDatasetsSuccess(res.data));
  }
}

function* createDataset(action): SagaIterator {
  const uploadResult = yield call(api.uploadFile, action.data.namespace, action.data.files.files);

  if (!uploadResult.err) {
    const createResult = yield* checkedApiCall(
      api.createDataset,
      action.data.namespace,
      action.data.metadata,
      uploadResult.data,
    );
    if (!createResult.err) {
      yield call(showSuccess, `Successfully created dataset: ${action.data.metadata.name}`);
      yield put(actions.setActiveModal(null));
      yield put(actions.fetchDatasets());
    }
  }
}

function* deleteDataset(action): SagaIterator {
  const res = yield* checkedApiCall(api.deleteDataset, action.coords);

  if (!res.err) {
    yield call(showSuccess, `Deleted dataset: ${action.coords.id}`);
    yield put(actions.fetchDatasets());
  }
}

export function* manageDatasets(): SagaIterator {
  yield fork(watch(constants.FETCH_DATASETS, fetchDatasets));
  yield fork(watch(constants.CREATE_DATASET, createDataset));
  yield fork(watch(constants.DELETE_DATASET, deleteDataset));
}
