import * as constants from "./constants";

export function clickNewDataset() {
  console.log("hello!");
}

export function fetchDatasets() {
  return {
    type: constants.FETCH_DATASETS,
  }
}

export function fetchDatasetsSuccess() {
  return {
    type: constants.FETCH_DATASETS_SUCCESS,
  }
}
