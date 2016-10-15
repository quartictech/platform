import { call } from "redux-saga/effects";
import { delay } from "redux-saga";
import request from "utils/request";
import { apiRootUrl } from "../../../utils.js";

const mapboxToken = "pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw";

export default function* (action) {
  yield call(delay, 500); // Debounce
  const [layerResults, placeResults] = yield [
    call(fetchLayers, action.query),
    call(fetchPlaces, action.query),
  ];

  // TODO: error-checking

  const response = {
    results: {
      layers: {
        name: "Layers",
        results: layerResults.data.filter(x => !x.live).map(unpackLayer),
      },
      live: {
        name: "Live layers",
        results: layerResults.data.filter(x => x.live).map(unpackLayer),
      },
      places: {
        name: "Places",
        results: unpackResults(placeResults.data),
      },
    },
  };
  yield call(action.callback, response);
}

function* fetchLayers(query) {
  const requestURL = `${apiRootUrl}/layer?query=${encodeURI(query)}`;
  const results = yield call(request, requestURL, { method: "GET" });
  return results;
}

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
