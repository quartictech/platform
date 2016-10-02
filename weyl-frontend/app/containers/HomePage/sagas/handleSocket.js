import { take, call, put, select } from "redux-saga/effects";
import { eventChannel } from "redux-saga";
import { wsRoot } from "../../../../weylConfig.js";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";

const createSocketChannel(socket) =>
  eventChannel(emit => {
    socket.onmessage = (event) => emit(JSON.parse(event.data)); // eslint-disable-line no-param-reassign
    socket.onclose = (event) => console.log("onclose", event);
    return () => socket.close();
  });

function* handleLayerUpdate(msg) {
  const layer = yield select(selectors.selectLayer(msg.layerId));
  if (layer) {
    yield put(actions.layerSetData(msg.layerId, msg.featureCollection));
  } else {
    console.warn(`Recieved unactionable update for layerId ${msg.layerId}`);
  }
}

const createNotification = (title, body) => {
  const n = new Notification(title, { body });
  setTimeout(n.close.bind(n), 5000);
};

function* handleAlert(msg) {
  yield call(createNotification, msg.title, msg.body);
}

function* handleSocketPushes(socket) {
  const chan = yield call(createSocketChannel, socket);
  while (true) {
    const msg = yield take(chan);
    switch (msg.type) {
      case "LayerUpdate":
        yield* handleLayerUpdate(msg);
        break;
      case "Alert":
        yield* handleAlert(msg);
        break;
      default:
        console.warn(`Unrecognised message type ${msg.type}`);
        break;
    }
  }
}

const reportLayerSubscriptionChange = (socket, type, layerId) =>
  socket.send(JSON.stringify({ type, layerId }));

function* reportLayerSubscriptionChanges(socket) {
  // We need the backend to be in a consistent state, and that's only possible
  // if it receives subscribe/unsubscribe in the order that they occurred.
  // TODO: maybe we should just send the absolute state each time?
  const channel = yield actionChannel([constants.LAYER_CREATE, constants.LAYER_CLOSE]);
  while (true) {
    const action = yield take(channel);
    switch (action.type) {
      case constants.LAYER_CREATE:
        if (action.live) {
          yield call(reportLayerSubscriptionChange, socket, "subscribe", action.id);
        }
        break;
      case constants.LAYER_CLOSE: {
        const layer = yield select(selectors.selectLayer(action.layerId));
        if (layer.live) {
          yield call(reportLayerSubscriptionChange, socket, "unsubscribe", action.layerId);
        }
        break;
      }
      default:
        break;  // Should never get here
    }
  }
}

export default function* handleSocket() {
  // TODO: should be yielded?
  const socket = new WebSocket(`${wsRoot}/live-ws`);
  // TODO: error handling

  yield [
    handleSocketPushes(socket),
    reportLayerSubscriptionChanges(socket),
  ];
}
