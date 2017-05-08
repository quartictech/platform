import { takeLatest, fork } from "redux-saga/effects";

import * as constants from "../constants";
import manageSocket from "./manageSocket";
import performComputation from "./performComputation";
import layerExport from "./layerExport";

function watch(action, generator) {
  return function* () {
    yield takeLatest(action, generator);
  };
}

function prepare(generator) {
  return function* () {
    yield fork(generator);
  };
}

export default [
  prepare(manageSocket),
  prepare(watch(constants.COMPUTATION_START, performComputation)),
  prepare(watch(constants.LAYER_EXPORT, layerExport)),
];
