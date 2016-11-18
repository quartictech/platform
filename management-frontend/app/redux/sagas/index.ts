import { LOCATION_CHANGE } from "react-router-redux";
import { take, call, put, fork, cancel } from "redux-saga/effects";
import { SagaIterator } from "redux-saga";
declare var Notification: any;

function prepare(generator): () => SagaIterator {
  return function* () {
    const forked = yield fork(generator);
    yield take(LOCATION_CHANGE);
    yield cancel(forked);
  };
}

function* askForNotificationPermission() {
  yield call(Notification.requestPermission);
}

const sagas: (() => SagaIterator)[] = [
  prepare(askForNotificationPermission),
];

export { sagas }
