import * as constants from "./constants";
import { IDatasetMetadata } from "../models";

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

export function searchDatasets(search) {
  return {
    type: constants.SEARCH_DATASETS,
    search: search.toLowerCase()
  };
}

export function createDataset(metadata: IDatasetMetadata, files: any[]) {
    return {
      type: constants.CREATE_DATASET,
      data: {
        metadata,
        files
      }
    };
}

export function setActiveModal(activeModal) {
  return {
    type: constants.UI_SET_ACTIVE_MODAL,
    activeModal
  };
}
