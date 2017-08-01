import { take, call, put, fork } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { toaster } from "../../containers/App/toaster";

import { Intent } from "@blueprintjs/core";
import { push } from "react-router-redux";

import { QUARTIC_XSRF } from "../../helpers/Utils";

function showError(message) {
  toaster.show({
    iconName: "warning-sign",
    intent: Intent.DANGER,
    message,
  });
}

function* checkedApiCall(apiFunction, ...args): SagaIterator {
  const res = yield call(apiFunction, ...args);

  if (! res.err) {
    yield res;
  }

  if (res.err && res.err.status === 401) {
    yield put(actions.logout());
  }

  if (res.err) {
    showError(res.err.message);
    return res;
  }

  return res;
}

function showSuccess(message) {
  toaster.show({
    intent: Intent.SUCCESS,
    message,
  });
}

function* watchLogout(): SagaIterator {
  while (true) {
    yield take(constants.LOGOUT);
    showError("Logged out");
    localStorage.removeItem(QUARTIC_XSRF);
    yield put(push("/login"));
  }
}

function* watchLoadDatasets(): SagaIterator {
  while (true) {
    yield take(constants.FETCH_DATASETS);
    const res = yield* checkedApiCall(api.fetchDatasets);

    if (!res.err) {
      yield put(actions.fetchDatasetsSuccess(res.data));
    }
  }
}

function* watchLoginGithub(): SagaIterator {
  while (true) {
    const action = yield take(constants.LOGIN_GITHUB);
    const res = yield call(api.githubAuth, action.code);

    if (! res.err) {
      localStorage.setItem(QUARTIC_XSRF, res.xsrfToken);
      yield put(push("/"));
    } else {
      showError("Couldn't authenticate");
      yield put(push("/login"));
    }
  }
}

function* watchDeleteDataset(): SagaIterator {
  while (true) {
    const action = yield take(constants.DELETE_DATASET);
    const res = yield* checkedApiCall(api.deleteDataset, action.coords);

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
      const createResult = yield* checkedApiCall(
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
  yield fork(watchLogout);
}
