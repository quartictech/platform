import { fork, put } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { checkedApiCall, watch } from "./utils";

function* fetchPipeline(_action): SagaIterator {
  const res = yield* checkedApiCall(api.fetchDag);
  if (!res.err) {
    yield put(actions.fetchPipelineSuccess(res.data));
  }
}

export function* managePipeline(): SagaIterator {
  yield fork(watch(constants.FETCH_PIPELINE, fetchPipeline));
}
