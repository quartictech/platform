import { call, put } from "redux-saga/effects";

import request from "utils/request";
import * as actions from "../actions";
import { apiRootUrl } from "../../../utils.js";

export default function* performComputation(action) {
  const requestURL = `${apiRootUrl}/compute`;
  const results = yield call(request, requestURL, {
    method: "POST",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(action.computation),
  });

  if (!results.err) {
    yield put(actions.layerCreate(results.data));
  }

  yield put(actions.computationEnd());
}
