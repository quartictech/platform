import { take, call, put, fork, cancel, select } from "redux-saga/effects";
import { delay, takeLatest } from "redux-saga";


import { LOCATION_CHANGE } from "react-router-redux";
import request from "utils/request";
import * as constants from "./constants";
import * as actions from "./actions";
import * as selectors from "./selectors";

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
          results: results.data.filter(x => !x.live).map(x => ({ ...x, title: x.name })),
        },
        live: {
          name: "Live layers",
          results: results.data.filter(x => x.live).map(x => ({ ...x, title: x.name })),
        },
      },
    };
    yield put(actions.searchDone(response, action.callback));
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
      yield put(actions.layerCreate(results2.data));
    }
  }
}

function* numericAttributes(action) {
  console.log("Fetching numeric attributes");
  const requestURL = `${apiRoot}/layer/numeric_values/${action.layerId}`;
  const results = yield call(request, requestURL, {
    method: "GET",
  });

  yield put(actions.loadNumericAttributesDone(results.data));
}

function* geofenceSave(action) {
  console.log("saving geofence");
  const requestURL = `${apiRoot}/geofence/`;
  yield call(request, requestURL, {
    method: "PUT",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      features: action.geofence.geojson,
      type: action.geofence.type,
    }),
  });

  yield put(actions.geofenceSaveDone());
}

// ////////////////////////

function* getNotifications() {
  const results = yield call(request, `${apiRoot}/geofence/violations`, {
    method: "GET",
  });
  if (!results.err) {
    yield put(actions.notificationsUpdate(results.data));
  }
  return results;
}

function displayNewNotifications(allNotifications, oldNotifications) {
  Object.keys(allNotifications)
    .filter(k => !(k in oldNotifications))
    .forEach(k => {
      const n = new Notification("Geofence violation", {
        body: allNotifications[k].message,
        tag: k,
      });
      setTimeout(n.close.bind(n), 5000);
    });
}

function* pollForStuff() {
  yield* getNotifications();  // Seed so we don't display historical notifications

  while (true) {
    yield call(delay, 2000);

    const oldNotifications = yield select(selectors.selectNotifications());
    const results = yield* getNotifications();

    if (!results.err) {
      displayNewNotifications(results.data, oldNotifications);
    }
  }
}

function* searchWatcher() {
  yield* takeLatest(constants.SEARCH, search);
}

function* computationWatcher() {
  yield* takeLatest(constants.BUCKET_COMPUTATION_START, bucketComputation);
}

function* numericAttributesWatcher() {
  yield* takeLatest(constants.NUMERIC_ATTRIBUTES_LOAD, numericAttributes);
}

function* geofenceWatcher() {
  yield* takeLatest(constants.GEOFENCE_EDIT_FINISH, geofenceSave);
}

// ////////////////////////

function* wtf() {
  const watcher = yield fork(pollForStuff);
  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

function* searchData() {
  const watcher = yield fork(searchWatcher);
  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

function* computationData() {
  const watcher = yield fork(computationWatcher);
  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

function* numericAttributesData() {
  const watcher = yield fork(numericAttributesWatcher);
  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

function* geofenceData() {
  const watcher = yield fork(geofenceWatcher);
  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

// ////////////////////////

export default [
  wtf,
  searchData,
  computationData,
  numericAttributesData,
  geofenceData,
];
