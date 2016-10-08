import { take, call, fork, cancel, put, select } from "redux-saga/effects";
import { takeLatest, delay } from "redux-saga";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";
import request from "utils/request";
import { apiRootUrl } from "../../../utils.js";
const _ = require("underscore");

  // TODO: how to keep this list in-sync with things that affect selection?
const actionTypesThatAffectSelection = [constants.MAP_MOUSE_CLICK];

const fetch = (body) => request(`${apiRootUrl}/aggregates/histogram`, {
  method: "POST",
  headers: {
    "Accept": "application/json",
    "Content-Type": "application/json",
  },
  body: JSON.stringify(body),
});

function* fetchAndDispatch() {
  const selection = yield select(selectors.selectSelectionView());
  const results = yield call(fetch,
    _.chain(selection.features).map(f => f.properties["_id"]).value());

  if (!results.err) {
    yield put(actions.aggregatesLoaded(results.data));
  } else {
    console.warn(results);
    yield put(actions.aggregatesFailedToLoad());
  }
}

export default function* () {
  let lastTask;
  while (true) {
    // We rely on the reducer to only change the lifecycle state to AGGREGATES_REQUIRED
    // when new data is required.  This mechanim prevents every single map-click (etc.)
    // from triggering a new server request.
    const action = yield take(actionTypesThatAffectSelection);
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
