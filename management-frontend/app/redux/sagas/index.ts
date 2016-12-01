import { take, call, put, fork } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import { fetchDatasets, uploadFile, createDataset } from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { toaster } from "../../containers/App/toaster";

import { Intent } from "@blueprintjs/core";

function showError(message) {
  toaster.show({
    iconName: "warning-sign",
    intent: Intent.DANGER,
    message
  });
}

function showSuccess(message) {
  toaster.show({
    intent: Intent.SUCCESS,
    message,
  });
}

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
    const uploadResult = yield call(uploadFile, action.data.files.files);

    if (!uploadResult.err) {
      const createResult = yield call(createDataset, action.data.metadata,
        uploadResult.data, action.data.files.fileType);
      if (! createResult.err) {
        yield call(showSuccess, `Successfully created dataset: ${action.data.metadata.name}`);
        yield put(actions.setActiveModal(null));
        yield put(actions.fetchDatasets());
      } else {
          yield call(showError, `Error while creating dataset: ${createResult.err.message}`);
      }
    } else {
      yield call(showError, `Error while uploading file: ${uploadResult.err.message}`);
    }
  }
}

export function* sagas(): SagaIterator {
  yield fork(watchLoadDatasets);
  yield fork(watchCreateDataset);
}
