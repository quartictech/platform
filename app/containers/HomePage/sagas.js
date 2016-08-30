import { take, call, put, select, fork, cancel } from 'redux-saga/effects';
import { takeEvery, takeLatest } from 'redux-saga'

import { SEARCH } from './constants';
import { LOCATION_CHANGE } from 'react-router-redux';
import request from 'utils/request';
import { searchDone } from './actions';

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
          results: results.data.map((item) => { return {title: item.name, description: item.description, id: item.id.id}})
        }
      }
    }
    yield put(searchDone(response, action.callback));
  }
}

export function* searchWatcher() {
  yield* takeLatest(SEARCH, search);
}

export function* searchData() {
  console.log("Search watcher");
  const watcher = yield fork(searchWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}


export default [
    searchData
]
