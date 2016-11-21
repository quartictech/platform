import * as constants from "./constants";

export function clickNewDataset() {
}

export function fetchDatasets() {
  return {
    type: constants.FETCH_DATASETS,
  };
}

export function fetchDatasetsSuccess(data) {
  return {
    type: constants.FETCH_DATASETS_SUCCESS,
    data,
  };
}
