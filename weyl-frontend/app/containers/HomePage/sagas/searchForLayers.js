import { call, put } from "redux-saga/effects";
import * as actions from "../actions";
import request from "utils/request";
import { apiRootUrl } from "../../../utils.js";

export default function* (action) {
  console.log("Executing search");
  const requestURL = `${apiRootUrl}/layer?query=${encodeURI(action.query)}`;
  const results = yield call(request, requestURL, {
    method: "GET",
  });

  if (!results.err) {
    const response = {
      success: true,
      results: {
        layers: {
          name: "Layers",
          results: results.data.filter(x => !x.live).map(x => ({ ...x, title: x.name })),
        },
        live: {
          name: "Live layers",
          results: results.data.filter(x => x.live).map(x => ({ ...x, title: x.name })),
        },
      },
    };
    yield put(actions.searchDone(response, action.callback));
  }
}
