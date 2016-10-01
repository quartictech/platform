import { call, put, select } from "redux-saga/effects";
import { delay } from "redux-saga";
import request from "utils/request";
import { apiRoot } from "../../../../weylConfig.js";
import * as actions from "../actions";
import * as selectors from "../selectors";

function* getNotifications() {
  const results = yield call(request, `${apiRoot}/geofence/violations`, {
    method: "GET",
  });
  if (!results.err) {
    yield put(actions.notificationsUpdate(results.data));
  }
  return results;
}

function displayNewNotifications(allNotifications, oldNotifications) {
  Object.keys(allNotifications)
    .filter(k => !(k in oldNotifications))
    .forEach(k => {
      const n = new Notification("Geofence violation", {
        body: allNotifications[k].message,
        tag: k,
      });
      setTimeout(n.close.bind(n), 5000);
    });
}

export default function* pollForNotifications() {
  yield* getNotifications();  // Seed so we don't display historical notifications

  while (true) {
    yield call(delay, 2000);

    const oldNotifications = yield select(selectors.selectNotifications());
    const results = yield* getNotifications();

    if (!results.err) {
      displayNewNotifications(results.data, oldNotifications);
    }
  }
}
