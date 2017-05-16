import {
  call,
  put,
  takeLatest,
} from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { toaster } from "../../containers/App/toaster";

import { Intent } from "@blueprintjs/core";

const showError = (message) => toaster.show({
    iconName: "warning-sign",
    intent: Intent.DANGER,
    message
});

function* fetchAssets(): SagaIterator {
  yield put(actions.assets.beganLoading());
  try {
    const assets = yield call(api.fetchAssets);
    yield put(actions.assets.loaded(assets));
  } catch (e) {
    showError("Error loading assets.");
    yield put(actions.assets.failedToLoad());
  }
}

export function* sagas(): SagaIterator {
  yield takeLatest(constants.ASSETS.required, fetchAssets);
}
