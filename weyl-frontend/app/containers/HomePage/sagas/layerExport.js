import { call } from "redux-saga/effects";

import request from "utils/request";
import { apiRootUrl } from "../../../utils.js";

import { showToast } from "../toaster";


export default function* layerExport(action) {
  const requestURL = `${apiRootUrl}/export`;
  const results = yield call(request, requestURL, {
    method: "POST",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      layerId: action.layerId,
    }),
  });

  if (!results.err) {
    yield call(showToast, {
      title: "Layer export complete",
      body: results.data.message,
      icon: "export",
    });
  } else {
    yield call(showToast, {
      title: "Error while exporting layer",
      body: results.err.message,
      icon: "export",
      level: "SEVERE",
    });
  }
}
