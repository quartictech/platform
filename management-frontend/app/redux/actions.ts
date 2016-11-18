import * as constants from "./constants";

export function clickNewDataset() {
  console.log("hello!");
}

export function fetchDatasets() {
  console.log("fetch");
  return {
    type: constants.FETCH_DATASETS,
  }
}

export function fetchDatasetsSuccess(data) {
  console.log("sz");
  console.log(data);
  return {
    type: constants.FETCH_DATASETS_SUCCESS,
    data
  }
}
