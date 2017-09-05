import { fork, put } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { checkedApiCall, watch } from "./utils";

function* fetchBuilds(_action): SagaIterator {
  const res = yield* checkedApiCall(api.fetchFeed);
  if (!res.err) {
    yield put(actions.fetchFeedSuccess(res.data));
  }
}

export function* manageFeed(): SagaIterator {
  yield fork(watch(constants.FETCH_FEED, fetchBuilds));
}
