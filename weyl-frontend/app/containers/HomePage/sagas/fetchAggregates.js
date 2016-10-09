import { take, call, fork, cancel, put, select } from "redux-saga/effects";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";
import request from "utils/request";
import { apiRootUrl } from "../../../utils.js";
const _ = require("underscore");

  // TODO: how to keep this list in-sync with things that affect selection?
const actionTypesThatAffectSelection = [constants.MAP_MOUSE_CLICK];

const fetchHistogram = (body) => request(`${apiRootUrl}/aggregates/histogram`, {
  method: "POST",
  headers: {
    "Accept": "application/json",
    "Content-Type": "application/json",
  },
  body: JSON.stringify(body),
});

const fetchTimeSeries = (body) => request(`${apiRootUrl}/attributes/time_series`, {
  method: "POST",
  headers: {
    "Accept": "application/json",
    "Content-Type": "application/json",
  },
  body: JSON.stringify(body),
});

function* fetchAndDispatch() {
  const selection = yield select(selectors.selectSelectionView());
  const featureIds = _.chain(selection.features).map(f => f.properties["_id"]).value(); // eslint-disable-line dot-notation
  const histogramResults = yield call(fetchHistogram, featureIds);
  const timeSeriesResults = yield call(fetchTimeSeries, featureIds);

  if (!histogramResults.err && !timeSeriesResults.err) {
    const results = {
      histogram: histogramResults.data,
      timeSeries: timeSeriesResults.data,
    }
    yield put(actions.aggregatesLoaded(results));
  } else {
    console.warn(histogramResults);
    console.warn(timeSeriesResults);
    yield put(actions.aggregatesFailedToLoad());
  }
}

export default function* () {
  let lastTask;
  while (true) {
    // We rely on the reducer to only change the lifecycle state to AGGREGATES_REQUIRED
    // when new data is required.  This mechanim prevents every single map-click (etc.)
    // from triggering a new server request.
    yield take(actionTypesThatAffectSelection);
    const state = yield select(selectors.selectAggregatesLifecycleState());
    if (state === "AGGREGATES_REQUIRED") {
      // We cancel *before* dispatching the lifecycle-change action.  This is to
      // mitigate the race-condition where a previously forked fetch completes
      // and dispatches outdated data back to the reducer.  The lifecycle
      // state-machine can simply ignore it.
      if (lastTask) {
        yield cancel(lastTask);
      }
      yield put(actions.aggregatesLoading());
      lastTask = yield fork(fetchAndDispatch);
    }
  }
}
