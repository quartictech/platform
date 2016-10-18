import { call, put, select } from "redux-saga/effects";
import request from "utils/request";
import * as actions from "../actions";
import * as selectors from "../selectors";
import { apiRootUrl } from "../../../utils.js";

export default function* () {
  const geofence = yield select(selectors.selectGeofence);

  const requestURL = `${apiRootUrl}/geofence/`;
  yield call(request, requestURL, {
    method: "PUT",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      features: geofence.layerId === null ? geofence.geojson : null,
      type: geofence.type,
      layerId: geofence.layerId,
      bufferDistance: geofence.bufferDistance,
    }),
  });

  yield put(actions.geofenceSaveDone());
}
