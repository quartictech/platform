import { call, select } from "redux-saga/effects";
import { delay } from "redux-saga";
import * as _ from "underscore";
import request from "utils/request";
import * as selectors from "../selectors";
import { apiRootUrl, mapboxToken } from "../../../utils.js";

export default function* (action) {
  yield call(delay, 500); // Debounce

  const [layerResults, placeResults] = yield [
    call(fetchLayers, action.query),
    call(fetchPlaces, action.query)
  ];

  const results = Object.assign({},
    {
      layers: {
        name: "Layers",
        results: layerResults.filter(x => !x.live).map(unpackLayer),
      },
      live: {
        name: "Live layers",
        results: layerResults.filter(x => x.live).map(unpackLayer),
      },
    },
    placeResults.err ? {} : {
      places: {
        name: "Places",
        results: unpackResults(placeResults.data),
      },
    },
  );

  yield call(action.callback, results);
}

function* fetchLayers(query) {
  const layerResults = yield select(selectors.selectLayerList);
  return _.map(layerResults, (v, id) => ({ ...v, id, }))
    .filter(r => includes(r.metadata.name, query));
}

const includes = (str, substr) => str.toLowerCase().includes(substr.toLowerCase());

function* fetchPlaces(query) {
  const requestURL = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURI(query)}.json?access_token=${mapboxToken}`;
  const results = yield call(request, requestURL, { method: "GET" });
  return results;
}

const unpackResults = (results) => (
  results.features.map(f => ({
    title: f.text,
    description: f.place_name,
    category: "place",
    payload: f.center,
  }))
);

const unpackLayer = (layer) => ({
  title: layer.metadata.name,
  description: layer.metadata.description,
  category: "layer",
  payload: layer,
});
