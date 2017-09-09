import { call, fork, put } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";
import { push } from "react-router-redux";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";

import { QUARTIC_XSRF } from "../../helpers/Utils";

import { checkedApiCall, watch } from "./utils";

function* fetchProfile(): SagaIterator {
  const res = yield* checkedApiCall(api.fetchProfile);
  if (!res.err) {
    yield put(actions.userFetchProfileSuccess(res.data));
  }
}

function* logout(_action): SagaIterator {
  localStorage.removeItem(QUARTIC_XSRF);
  yield put(push("/login"));
}

function* loginGithub(action): SagaIterator {
  const res = yield call(api.githubAuth, action.code, action.state);

  if (!res.err) {
    localStorage.setItem(QUARTIC_XSRF, res.xsrfToken);
    yield put(actions.userLoginSuccess());
    yield put(push("/"));
    yield* fetchProfile();  // TODO - what if profile fetching fails?
  } else {
    yield put(push("/login"));  // TODO - go to a "you are noob, try again page"
  }
}

function* apolloQueryError(action) {
  if (action.error.status === 401) {
    yield* logout(action);
  }
}

export function* manageUser(): SagaIterator {
  if (localStorage.getItem(QUARTIC_XSRF)) {
    yield put(actions.userLoginSuccess());
    yield fork(fetchProfile);   // Do this async
  }

  yield fork(watch(constants.USER_LOGIN_GITHUB, loginGithub));
  yield fork(watch(constants.USER_LOGOUT, logout));
  yield fork(watch("APOLLO_QUERY_ERROR", apolloQueryError));
}
