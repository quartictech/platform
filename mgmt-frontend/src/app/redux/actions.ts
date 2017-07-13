import * as constants from "./constants";
import { IDatasetMetadata, IDatasetCoords, IFiles } from "../models";

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
    search: search.toLowerCase(),
  };
}

export function selectNamespace(namespace) {
  return {
    type: constants.SELECT_NAMESPACE,
    namespace: namespace,
  };
}

export function createDataset(namespace: string, metadata: IDatasetMetadata, files: IFiles) {
    return {
      type: constants.CREATE_DATASET,
      data: {
        namespace,
        metadata,
        files,
      },
    };
}

export function deleteDataset(coords: IDatasetCoords) {
    return {
      type: constants.DELETE_DATASET,
      coords,
    };
}

export function createDatasetError(error: string) {
  return {
    type: constants.CREATE_DATASET_ERROR,
    error,
  };
}

export function setActiveModal(activeModal) {
  return {
    type: constants.UI_SET_ACTIVE_MODAL,
    activeModal,
  };
}
