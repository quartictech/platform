import { take, call, put, fork, cancel } from "redux-saga/effects";
import { takeLatest } from "redux-saga";

import { SEARCH, BUCKET_COMPUTATION_START, NUMERIC_ATTRIBUTES_LOAD, GEOFENCE_EDIT_FINISH } from "./constants";
import { LOCATION_CHANGE } from "react-router-redux";
import request from "utils/request";
import { searchDone, layerCreate, loadNumericAttributesDone } from "./actions";

import { apiRoot } from "../../../weylConfig.js";

function* search(action) {
  console.log("Executing search");
  const requestURL = `${apiRoot}/layer?query=${encodeURI(action.query)}`;
  const results = yield call(request, requestURL, {
    method: "GET",
  });

  if (!results.err) {
    const response = {
      success: true,
      results: {
        layers: {
          name: "Layers",
          results: results.data.filter(x => !x.live).map(x => ({ ...x, title: x.name }))
        },
        live: {
          name: "Live layers",
          results: results.data.filter(x => x.live).map(x => ({ ...x, title: x.name }))
        },
      },
    };
    yield put(searchDone(response, action.callback));
  }
}

function* bucketComputation(action) {
  console.log(action);
  const requestURL = `${apiRoot}/layer/compute`;
  const results = yield call(request, requestURL, {
    method: "PUT",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(action.computation),
  });

  if (!results.err) {
    console.log(results);
    const requestURL2 = `${apiRoot}/layer/metadata/${results.data}`;
    const results2 = yield call(request, requestURL2, {
      method: "GET",
    });

    if (!results2.err) {
      yield put(layerCreate(results2.data));
    }
  }
}

function* numericAttributes(action) {
  console.log("Fetching numeric attributes");
  const requestURL = `${apiRoot}/layer/numeric_values/${action.layerId}`;
  const results = yield call(request, requestURL, {
    method: "GET",
  });

  yield put(loadNumericAttributesDone(results.data));
}

function* geofenceSave(action) {
  console.log("saving geofence");
  const requestURL = `${apiRoot}/geofence/`;
  const results = yield call(request, requestURL, {
    method: "PUT",
    body: JSON.stringify({
      features: action.geojson,
      type: "EXCLUDE",
    }),
  });

  yield put(loadNumericAttributesDone(results.data));
}

export function* searchWatcher() {
  yield* takeLatest(SEARCH, search);
}

export function* computationWatcher() {
  yield* takeLatest(BUCKET_COMPUTATION_START, bucketComputation);
}

export function* numericAttributesWatcher() {
  yield* takeLatest(NUMERIC_ATTRIBUTES_LOAD, numericAttributes);
}

export function* geofenceWatcher() {
  yield* takeLatest(GEOFENCE_EDIT_FINISH, geofenceSave);
}

export function* searchData() {
  const watcher = yield fork(searchWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

export function* computationData() {
  yield fork(computationWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(computationWatcher);
}

export function* numericAttributesData() {
  yield fork(numericAttributesWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(numericAttributesWatcher);
}

export function* geofenceData() {
  yield fork(geofenceWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(geofenceWatcher);
}


export default [
  searchData,
  computationData,
  numericAttributesData,
  geofenceData,
];
