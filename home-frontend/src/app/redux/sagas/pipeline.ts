import { fork, put } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { checkedApiCall, watch } from "./utils";

function* fetchPipeline(action): SagaIterator {
  const res = yield* checkedApiCall(api.fetchDag, action.build);
  if (!res.err) {
    yield put(actions.fetchPipelineSuccess(res.data));
  } else if (res.err.status === 404) {
    yield put(actions.fetchPipelineNotFound());
  }
}

export function* managePipeline(): SagaIterator {
  yield fork(watch(constants.FETCH_PIPELINE, fetchPipeline));
}
