import { take, takem, call, put, select, race, actionChannel } from "redux-saga/effects";
import { eventChannel, END, delay } from "redux-saga";
import { wsRoot } from "../../../../weylConfig.js";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";


const sendMessage = (socket, msg) => socket.send(JSON.stringify(msg));

function* reportStatus(socket) {
  const subscribedLiveLayerIds = yield select(selectors.selectLiveLayerIds());

  const msg = {
    type: "ClientStatus",
    subscribedLiveLayerIds,
  };

  yield call(sendMessage, socket, msg);
}

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

function* handleMessages(channel) {
  while (true) {
    const msg = yield take(channel);
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

function* watchLayerChanges(socket) {
  while (true) {
    yield take([constants.LAYER_CREATE, constants.LAYER_CLOSE]);
    yield* reportStatus(socket);
  }
}

const createSocket = () => new WebSocket(`${wsRoot}/live-ws`);

const createSocketChannel = (socket) =>
  eventChannel(emit => {
    socket.onopen = (event) => emit("open");
    socket.onmessage = (event) => emit(JSON.parse(event.data)); // eslint-disable-line no-param-reassign
    socket.onclose = (event) => emit(END);
    return () => socket.close();
  });

export default function* () {
  while (true) {
    const socket = yield call(createSocket);
    const channel = yield call(createSocketChannel, socket);

    const result = yield takem(channel);  // First result should be "open"
    if (result !== END) {
      console.log("socketUp");

      yield* reportStatus(socket);

      yield race({
        watchLayerChanges: watchLayerChanges(socket),
        handleMessages: handleMessages(channel),
      });
    }

    console.log("socketDown");

    // TODO: dispatch socketDown action

    yield call(delay, 1000); // Rate-limit reconnection attempts
  }
}
