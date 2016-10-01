import { take, call, put, select } from "redux-saga/effects";
import { eventChannel } from "redux-saga";
import { wsRoot } from "../../../../weylConfig.js";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";

function createSocketChannel(socket) {
  return eventChannel(emit => {
    socket.onmessage = (event) => emit(JSON.parse(event.data)); // eslint-disable-line no-param-reassign
    return () => socket.close();
  });
}

function* handleLayerUpdates(socket) {
  const chan = yield call(createSocketChannel, socket);
  while (true) {
    const msg = yield take(chan);
    const layer = yield select(selectors.selectLayer(msg.layerId));
    if (layer) {
      yield put(actions.layerSetData(msg.layerId, msg.featureCollection));
    } else {
      console.warn(`Recieved unactionable update for layerId ${msg.layerId}`);
    }
  }
}

function reportLayerSubscriptionChange(socket, type, layerId) {
  return new Promise(resolve => {
    socket.send(JSON.stringify({ type, layerId }));
    resolve();
  });
}

function* reportLayerSubscriptionChanges(socket) {
  while (true) {
    const action = yield take([constants.LAYER_CREATE, constants.LAYER_CLOSE]);
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
    handleLayerUpdates(socket),
    reportLayerSubscriptionChanges(socket),
  ];
}
