import { take, call, put, select, fork, cancel } from 'redux-saga/effects';

import { IMPORT_LAYER } from './constants';
import { LOCATION_CHANGE } from 'react-router-redux';
import request from 'utils/request';
import { importLayerDone } from './actions';

export function* importLayer() {
  const requestURL = "http://localhost:8080/api/layer/import/test"
  const layerId = yield call(request, requestURL, {
      method: 'PUT',
      body: JSON.stringify({'query': 'SELECT * from companies_by_lsoa_london'}),
      headers: { "Content-Type": "application/json"}
    })

  if (!layerId.err) {
    yield put(importLayerDone(layerId.data.id))
  }
}

export function* importLayerWatcher() {
  while (yield take(IMPORT_LAYER)) {
    yield call(importLayer);
  }
}

export function* layerData() {
  console.log("layerData");
  const watcher = yield fork(importLayerWatcher);

  yield take(LOCATION_CHANGE);
  yield cancel(watcher);
}

export default [
    layerData,
]
