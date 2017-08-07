import { call, fork, put, select } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";
import { push } from "react-router-redux";

import * as api from "../api";
import * as actions from "../actions";
import * as constants from "../constants";
import * as selectors from "../selectors";

import { QUARTIC_XSRF } from "../../helpers/Utils";

import { checkedApiCall, watch } from "./utils";

function* isLoggedIn(): SagaIterator {
  yield select(selectors.selectLoggedIn);
}

function* fetchProfile(): SagaIterator {
  const res = yield* checkedApiCall(api.fetchProfile);
  if (!res.err) {
    yield put(actions.userFetchProfileSuccess(res.data));
  }
}

function* fetchProfileIfLoggedIn(): SagaIterator {
  const loggedIn = yield* isLoggedIn();
  if (loggedIn) {
    yield* fetchProfile();
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

export function* manageUser(): SagaIterator {
  yield fork(fetchProfileIfLoggedIn);
  yield fork(watch(constants.USER_LOGIN_GITHUB, loginGithub));
  yield fork(watch(constants.USER_LOGOUT, logout));
}
