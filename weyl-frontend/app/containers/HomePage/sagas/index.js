import { take, call, put, fork, cancel } from "redux-saga/effects";
import { takeLatest } from "redux-saga";

import { LOCATION_CHANGE } from "react-router-redux";
import request from "utils/request";
import * as constants from "../constants";
import * as actions from "../actions";
import { apiRootUrl } from "../../../utils.js";

import search from "./search";
import manageSocket from "./manageSocket";
import fetchSelectionInfo from "./fetchSelectionInfo";
import saveGeofence from "./saveGeofence";

function* runLayerComputation(action) {
  const requestURL = `${apiRootUrl}/layer/compute`;
  const results = yield call(request, requestURL, {
    method: "PUT",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(action.computation),
  });

  if (!results.err) {
    const requestURL2 = `${apiRootUrl}/layer/metadata/${results.data}`;
    const results2 = yield call(request, requestURL2, {
      method: "GET",
    });

    if (!results2.err) {
      yield put(actions.layerCreate(results2.data));
    }
  }
}

function* askForNotificationPermission() {
  yield call(Notification.requestPermission);
}

// ////////////////////////

function watch(action, generator) {
  return function* () {
    yield* takeLatest(action, generator);
  };
}

function prepare(generator) {
  return function* () {
    const forked = yield fork(generator);
    yield take(LOCATION_CHANGE);
    yield cancel(forked);
  };
}

export default [
  prepare(askForNotificationPermission),
  prepare(manageSocket),
  prepare(fetchSelectionInfo),
  prepare(watch(constants.SEARCH, search)),
  prepare(watch(constants.LAYER_COMPUTATION_START, runLayerComputation)),
  prepare(watch(constants.GEOFENCE_EDIT_FINISH, saveGeofence)),
];
