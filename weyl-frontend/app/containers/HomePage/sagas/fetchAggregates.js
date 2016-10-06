import { take, call, put, select, race } from "redux-saga/effects";
import { takeLatest } from "redux-saga";
import * as constants from "../constants";
import * as actions from "../actions";
import * as selectors from "../selectors";

  // TODO: how to keep this list in-sync with things that affect selection?
const actionTypesThatAffectSelection = [constants.MAP_MOUSE_CLICK];


function* fetchFromServer() {
  const selection = yield select(selectors.selectSelectionFeatures());

  console.log("selection", selection);

  // TODO: send request
  // TODO: dispatch results
}

export default function* () {
  yield takeLatest(actionTypesThatAffectSelection, fetchFromServer);
}
