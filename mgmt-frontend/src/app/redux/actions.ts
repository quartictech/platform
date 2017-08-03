import * as constants from "./constants";
import { IDatasetMetadata, IDatasetCoords, IFiles } from "../models";

export function fetchDatasets() {
  return {
    type: constants.FETCH_DATASETS,
  };
}

export const userLoginGithub = (code, state) => ({
  type: constants.USER_LOGIN_GITHUB,
  code,
  state,  
});

export const userLogout = () => ({
  type: constants.USER_LOGOUT,
});

export const userLoginSuccess = () => ({
  type: constants.USER_LOGIN_SUCCESS,
});

export const userFetchProfileSuccess = (profile) => ({
  type: constants.USER_FETCH_PROFILE_SUCCESS,
  profile,
});

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
    namespace,
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

export function fetchPipeline() {
  return {
    type: constants.FETCH_PIPELINE,
  };
}

export function fetchPipelineSuccess(data) {
  return {
    type: constants.FETCH_PIPELINE_SUCCESS,
    data,
  };
}