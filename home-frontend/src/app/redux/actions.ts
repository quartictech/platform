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

export function fetchDatasetsSuccess(data) {
  return {
    type: constants.FETCH_DATASETS_SUCCESS,
    data,
  };
}

export function createDataset(metadata: IDatasetMetadata, files: IFiles) {
  return {
    type: constants.CREATE_DATASET,
    data: {
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

export function fetchPipeline(build: string) {
  return {
    type: constants.FETCH_PIPELINE,
    build,
  };
}

export function fetchPipelineSuccess(data) {
  return {
    type: constants.FETCH_PIPELINE_SUCCESS,
    data,
  };
}

export function fetchPipelineNotFound() {
  return {
    type: constants.FETCH_PIPELINE_NOT_FOUND,
  };
}

export function buildPipeline() {
  return {
    type: constants.BUILD_PIPELINE,
  };
}
