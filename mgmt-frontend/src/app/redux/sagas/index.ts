import { take, call, put, fork } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { toaster } from "../../containers/App/toaster";

import { Intent } from "@blueprintjs/core";
import { push } from 'react-router-redux';

function showError(message) {
  toaster.show({
    iconName: "warning-sign",
    intent: Intent.DANGER,
    message,
  });
}

function* checkedApiCall(apiFunction, ...args) {
  const res = yield call(apiFunction, ...args);

  if (! res.err) {
    return res;
  }

  if (res.err.message === "Unauthorized") {
    localStorage.removeItem("quartic-xsrf");
    yield put(push("/login"))
  }
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
    yield fork(checkedApiCall, api.fetchDatasets);
  }
}

function* watchLoginGithub(): SagaIterator {
  while (true) {
    const action = yield take(constants.LOGIN_GITHUB);
    const res = yield call(api.githubAuth, action.code);

    if (! res.err) {
      localStorage.setItem("quartic-xsrf", res.xssToken);
      yield put(push("/"));
    }
  }
}

function* watchDeleteDataset(): SagaIterator {
  while (true) {
    const action = yield take(constants.DELETE_DATASET);
    const res = yield call(api.deleteDataset, action.coords);

    if (! res.err) {
      yield call(showSuccess, `Deleted dataset: ${action.coords.id}`);
      yield put(actions.fetchDatasets());
    }
  }
}

function* watchCreateDataset(): SagaIterator {
  while (true) {
    const action = yield take(constants.CREATE_DATASET);
    const uploadResult = yield call(api.uploadFile, action.data.namespace, action.data.files.files);

    if (!uploadResult.err) {
      const createResult = yield call(
        api.createDataset,
        action.data.namespace,
        action.data.metadata,
        uploadResult.data,
        action.data.files.fileType,
      );
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
  yield fork(watchDeleteDataset);
  yield fork(watchCreateDataset);
  yield fork(watchLoginGithub);
}
