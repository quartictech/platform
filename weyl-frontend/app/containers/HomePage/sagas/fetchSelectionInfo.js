import { take, call, fork, cancel, put, select } from "redux-saga/effects";
import * as actions from "../actions";
import * as selectors from "../selectors";
import request from "utils/request";
import { apiRootUrl } from "../../../utils.js";
const _ = require("underscore");

const thingsToFetch = {
  featureAttributes: "/attributes",
  histograms: "/aggregates/histograms",
  timeSeries: "/attributes/time-series",
};

const fetchFromEndpoint = (features, endpoint) => request(`${apiRootUrl}${endpoint}`, {
  method: "POST",
  headers: {
    "Accept": "application/json",
    "Content-Type": "application/json",
  },
  body: JSON.stringify(features),
});

function* fetchAndDispatch() {
  const selectionIds = yield select(selectors.selectSelectionIds);
  const featureIds = _.flatten(_.values(selectionIds));

  const results = yield _.values(thingsToFetch)
    .map(endpoint => call(fetchFromEndpoint, featureIds, endpoint));

  if (_.some(results, r => r.err)) {
    results.forEach(r => console.warn(r));
    yield put(actions.selectionInfoFailedToLoad());
  } else {
    yield put(actions.selectionInfoLoaded(
      _.object(_.keys(thingsToFetch), results.map(r => r.data))
    ));
  }
}

export default function* () {
  let lastTask;
  for (;;) {
    // We rely on the reducer to only change the lifecycle state to INFO_REQUIRED
    // when new data is required.  This mechanim prevents every single map-click (etc.)
    // from triggering a new server request.
    yield take();
    const info = yield select(selectors.selectSelectionInfo);
    if (info.lifecycleState === "INFO_REQUIRED") {
      // We cancel *before* dispatching the lifecycle-change action.  This is to
      // mitigate the race-condition where a previously forked fetch completes
      // and dispatches outdated data back to the reducer.  The lifecycle
      // state-machine can simply ignore it.
      if (lastTask) {
        yield cancel(lastTask);
      }
      yield put(actions.selectionInfoLoading());
      lastTask = yield fork(fetchAndDispatch);
    }
  }
}
