import { fork, put } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { checkedApiCall, watch } from "./utils";

function* fetchBuilds(_action): SagaIterator {
  const res = yield* checkedApiCall(api.fetchBuilds);
  if (!res.err) {
    yield put(actions.fetchBuildsSuccess(res.data));
  }
}

export function* manageBuilds(): SagaIterator {
  yield fork(watch(constants.FETCH_BUILDS, fetchBuilds));
}
