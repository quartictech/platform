import { take, call, put, fork, cancel, select } from "redux-saga/effects";
import { delay, takeLatest } from "redux-saga";


import { LOCATION_CHANGE } from "react-router-redux";
import request from "utils/request";
import * as constants from "./constants";
import * as actions from "./actions";
import * as selectors from "./selectors";

import { apiRoot } from "../../../weylConfig.js";

function* searchForLayers(action) {
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

function* runBucketComputation(action) {
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

function* fetchNumericAttributes(action) {
  console.log("Fetching numeric attributes");
  const requestURL = `${apiRoot}/layer/numeric_values/${action.layerId}`;
  const results = yield call(request, requestURL, {
    method: "GET",
  });

  yield put(actions.loadNumericAttributesDone(results.data));
}

function* saveGeofence(action) {
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

function* pollForNotifications() {
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

function* pollForLiveLayerData() {
  while (true) {
    yield call(delay, 1000);

    const layerIds = yield select(selectors.selectLiveLayerIds());

    // TODO: run these in parallel
    for (const id of layerIds) {
      const results = yield call(request, `${apiRoot}/layer/live/${id}`, {
        method: "GET",
      });
      if (!results.err) {
        yield put(actions.layerSetData(id, results.data));
      }
    }
  }
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
  prepare(pollForNotifications),
  prepare(pollForLiveLayerData),
  prepare(watch(constants.SEARCH, searchForLayers)),
  prepare(watch(constants.BUCKET_COMPUTATION_START, runBucketComputation)),
  prepare(watch(constants.NUMERIC_ATTRIBUTES_LOAD, fetchNumericAttributes)),
  prepare(watch(constants.GEOFENCE_EDIT_FINISH, saveGeofence)),
];
