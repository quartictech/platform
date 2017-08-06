import { take, call, put, fork, select } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";
import * as selectors from "../selectors";

import { toaster } from "../../containers/App/toaster";

import { Intent } from "@blueprintjs/core";
import { push } from "react-router-redux";

import { QUARTIC_XSRF } from "../../helpers/Utils";

function* checkedApiCall(apiFunction, ...args): SagaIterator {
  const res = yield call(apiFunction, ...args);
  const loggedIn = yield select(selectors.selectLoggedIn);
  if (loggedIn && res.err && res.err.status === 401) {
    yield put(actions.userLogout());
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
    yield take(constants.USER_LOGOUT);
    localStorage.removeItem(QUARTIC_XSRF);
    yield put(push("/login"));
  }
}

function* watchFetchDatasets(): SagaIterator {
  while (true) {
    yield take(constants.FETCH_DATASETS);
    const res = yield* checkedApiCall(api.fetchDatasets);
    if (!res.err) {
      yield put(actions.fetchDatasetsSuccess(res.data));
    }
  }
}

function* watchFetchPipeline(): SagaIterator {
  while (true) {
    yield take(constants.FETCH_PIPELINE);
    const res = yield* checkedApiCall(api.fetchDag);
    if (!res.err) {
      yield put(actions.fetchPipelineSuccess(res.data));
    }
  }
}

function* fetchProfile(): SagaIterator {
  const res = yield* checkedApiCall(api.fetchProfile);
  if (!res.err) {
    yield put(actions.userFetchProfileSuccess(res.data));
  }
}

function* watchLoginGithub(): SagaIterator {
  while (true) {
    const action = yield take(constants.USER_LOGIN_GITHUB);
    const res = yield call(api.githubAuth, action.code, action.state);

    if (!res.err) {
      localStorage.setItem(QUARTIC_XSRF, res.xsrfToken);
      yield put(push("/"));
      yield* fetchProfile();  // TODO - what if profile fetching fails?
    } else {
      yield put(push("/login"));  // TODO - go to a "you are noob, try again page"
    }
  }
}

function* watchDeleteDataset(): SagaIterator {
  while (true) {
    const action = yield take(constants.DELETE_DATASET);
    const res = yield* checkedApiCall(api.deleteDataset, action.coords);

    if (!res.err) {
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
      if (!createResult.err) {
        yield call(showSuccess, `Successfully created dataset: ${action.data.metadata.name}`);
        yield put(actions.setActiveModal(null));
        yield put(actions.fetchDatasets());
      }
    }
  }
}

export function* sagas(): SagaIterator {
  yield fork(fetchProfile);
  yield fork(watchFetchDatasets);
  yield fork(watchFetchPipeline);
  yield fork(watchDeleteDataset);
  yield fork(watchCreateDataset);
  yield fork(watchLoginGithub);
  yield fork(watchLogout);
}
