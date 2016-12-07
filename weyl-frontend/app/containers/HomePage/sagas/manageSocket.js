import { take, takem, call, fork, cancel, put, select, race } from "redux-saga/effects";
import { eventChannel, END, delay } from "redux-saga";
import { wsUrl } from "../../../utils.js";
import { showToast } from "../toaster";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";
const _ = require("underscore");


const sendMessage = (socket, msg) => {
  socket.send(JSON.stringify(msg));
};

function* keepConnectionAlive(socket) {
  for (;;) {
    const msg = { type: "Ping" };
    yield call(sendMessage, socket, msg);
    yield call(delay, 30 * 1000);
  }
}

function* reportStatus(socket) {
  const selection = yield select(selectors.selectSelection);
  const geofence = yield select(selectors.selectGeofence);

  const msg = {
    type: "ClientStatus",
    subscribedLiveLayerIds: yield select(selectors.selectLiveLayerIds),
    selection: {
      entityIds: _.flatten(_.values(selection.ids)),
      seqNum: selection.seqNum,
    },
    geofence: {
      features: (geofence.layerId === null) ? geofence.editGeojson : null,
      layerId: geofence.layerId,
      type: geofence.type,
      bufferDistance: geofence.bufferDistance,
    },
  };

  yield call(sendMessage, socket, msg);
}

function* handleLayerUpdate(msg) {
  const layers = yield select(selectors.selectLayers);
  if (msg.layerId in layers.toJS()) {
    yield put(actions.layerSetData(msg.layerId, msg.featureCollection, msg.schema));
  } else {
    console.warn(`Recieved unactionable update for layerId ${msg.layerId}`);
  }
}

function* handleAlert(msg) {
  // TODO: make this a more generic filter mechanism
  const geofence = yield select(selectors.selectGeofence);
  if (!msg.title.startsWith("Geofence") || geofence.alertsEnabled) {
    yield call(showToast, msg);
  }
}

function* handleMessages(channel) {
  for (;;) {
    const msg = yield take(channel);
    switch (msg.type) {
      case "LayerUpdate":
        yield* handleLayerUpdate(msg);
        break;
      case "Alert":
        yield* handleAlert(msg);
        break;
      case "GeofenceViolationsUpdate":
        yield put(actions.geofenceSetViolatedGeofences(msg.violatingGeofenceIds));
        break;
      case "GeofenceGeometryUpdate":
        yield put(actions.geofenceSetGeometry(msg.featureCollection));
        break;
      case "SelectionDrivenUpdate":
        yield put(actions.subscriptionsPost(msg.subscriptionName, msg.seqNum, msg.data));
        break;
      default:
        console.warn(`Unrecognised message type ${msg.type}`);
        break;
    }
  }
}

function* watchSubscriptionChanges(socket) {
  let lastTask;
  for (;;) {
    const action = yield take();
    const selection = yield select(selectors.selectSelection);

    // TODO: cleanse this gross logic
    if (([constants.LAYER_CREATE, constants.LAYER_CLOSE, constants.GEOFENCE_EDIT_FINISH].indexOf(action.type) >= 0)) {
      if (lastTask) {
        yield cancel(lastTask);
      }
      lastTask = yield fork(reportStatus, socket);
    } else if (selection.seqNum > selection.latestSent) {
      if (lastTask) {
        yield cancel(lastTask);
      }
      yield put(actions.selectionSent(selection.seqNum));
      lastTask = yield fork(reportStatus, socket);
    }
  }
}

const createSocket = () => new WebSocket(wsUrl);

const createSocketChannel = (socket) =>
  eventChannel(emit => {
    socket.onopen = () => emit("open");                         // eslint-disable-line no-param-reassign
    socket.onmessage = (event) => emit(JSON.parse(event.data)); // eslint-disable-line no-param-reassign
    socket.onclose = () => emit(END);                           // eslint-disable-line no-param-reassign
    return () => socket.close();
  });

export default function* () {
  for (;;) {
    const socket = yield call(createSocket);
    const channel = yield call(createSocketChannel, socket);

    const result = yield takem(channel);  // First result should be "open"
    if (result !== END) {
      yield put(actions.connectionUp());
      yield* reportStatus(socket);

      yield race({
        watchSubscriptionChanges: watchSubscriptionChanges(socket),
        keepConnectionAlive: keepConnectionAlive(socket),
        handleMessages: handleMessages(channel),
      });
    }

    yield put(actions.connectionDown());
    yield call(delay, 1000); // Rate-limit reconnection attempts
  }
}
