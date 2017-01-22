import { take, fork, cancel } from "redux-saga/effects";
import { takeLatest } from "redux-saga";

import { LOCATION_CHANGE } from "react-router-redux";
import * as constants from "../constants";

import manageSocket from "./manageSocket";
import performComputation from "./performComputation";
import layerExport from "./layerExport";

function watch(action, generator) {
  return function* () {
    yield* takeLatest(action, generator);
  };
}

function prepare(generator) {
  return function* () {
    const forked = yield fork(generator);
    yield take(LOCATION_CHANGE);
    yield cancel(forked);
  };
}

export default [
  prepare(manageSocket),
  prepare(watch(constants.COMPUTATION_START, performComputation)),
  prepare(watch(constants.LAYER_EXPORT, layerExport)),
];
