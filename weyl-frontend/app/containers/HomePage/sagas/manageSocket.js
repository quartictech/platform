import { take, takem, call, fork, cancel, put, select, race } from "redux-saga/effects";
import { eventChannel, END, delay } from "redux-saga";
import { wsUrl } from "../../../utils.js";
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
  const subscribedLiveLayerIds = yield select(selectors.selectLiveLayerIds);
  const subscribedEntityIds = _.flatten(_.values(yield select(selectors.selectSelectedIds)));

  const msg = {
    type: "ClientStatus",
    subscribedLiveLayerIds,
    subscribedEntityIds,
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

const createNotification = (title, body) => {
  const n = new Notification(title, { body });
  setTimeout(n.close.bind(n), 5000);
};

function* handleAlert(msg) {
  // TODO: it's weird that the generic alert thing has to query the geofence state
  const geofence = yield select(selectors.selectGeofence);
  if (geofence.alertsEnabled) {
    yield call(createNotification, msg.title, msg.body);
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
        yield* put(actions.geofenceSetViolatedGeofences(msg.violatingGeofenceIds));
        break;
      case "GeofenceGeometryUpdate":
        yield* put(actions.geofenceSetGeometry(msg.featureCollection));
        break;
      case "ChartUpdate":
        yield put(actions.chartSetData(msg.timeseries));
        break;
      case "HistogramUpdate":
        yield put(actions.histogramSetData(msg.histograms));
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
    const info = yield select(selectors.selectSelectionInfo);
    // TODO: cleanse this gross logic
    if (([constants.LAYER_CREATE, constants.LAYER_CLOSE].indexOf(action.type) >= 0)) {
      if (lastTask) {
        yield cancel(lastTask);
      }
      lastTask = yield fork(reportStatus, socket);
    } else if (info.selectionNeedsSending) {
      if (lastTask) {
        yield cancel(lastTask);
      }
      yield put(actions.selectionStatusSent());
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
