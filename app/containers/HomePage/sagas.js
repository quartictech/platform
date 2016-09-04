import { take, call, put, select, fork, cancel } from 'redux-saga/effects';
import { takeEvery, takeLatest } from 'redux-saga'

import { SEARCH, BUCKET_COMPUTATION_START } from './constants';
import { LOCATION_CHANGE } from 'react-router-redux';
import request from 'utils/request';
import { searchDone, addItem } from './actions';

export function* search(action) {
  console.log("Executing search");
  const requestURL = "http://localhost:8080/api/layer?query=" + encodeURI(action.query)
  const results = yield call(request, requestURL, {
      method: 'GET'
    });

  if (!results.err) {
    let response = {
      success: true,
      results: {
        layers: {
          name: "Layers",
          results: results.data.map((item) => { return {title: item.name, name: item.name, description: item.description, id: item.id, stats: item.stats}})
        }
      }
    }
    yield put(searchDone(response, action.callback));
  }
}

export function* bucketComputation(action) {
  console.log(action);
  const requestURL = "http://localhost:8080/api/layer/compute";
  const results = yield call(request, requestURL, {
    method: 'PUT',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(action.computation)
  });

  if (! results.err) {
    console.log(results);
    const requestURL = "http://localhost:8080/api/layer/metadata/" + results.data;
    const results2 = yield call(request, requestURL, {
    method: 'GET',
  });

    if (! results2.err) {
      yield put(addItem(results2.data));
    }
  }
}

export function* searchWatcher() {
  yield* takeLatest(SEARCH, search);
}

export function* computationWatcher() {
  yield* takeLatest(BUCKET_COMPUTATION_START, bucketComputation);
}

export function* searchData() {
  const watcher = yield fork(searchWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

export function* computationData() {
  console.log("Computation watcher");
  const watcher = yield fork(computationWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(computationWatcher);
}


export default [
    searchData,
    computationData
]
