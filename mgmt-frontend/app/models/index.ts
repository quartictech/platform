export interface IDatasetCoords {
  namespace: string;
  id: string;
}

export interface IDatasetMetadata {
  name: string;
  description: string;
  attribution: string;
  icon?: string;
}

export interface IDatasetLocator {
  type: string;
}

export interface IFiles {
  files: any[];
  fileType: string;
}

export interface IDataset {
  metadata: IDatasetMetadata;
  locator: IDatasetLocator;
}

export type DatasetMap = { [namespace: string]: { [id: string]: IDataset } };

export interface ICreateDataset {
  metadata: IDatasetMetadata;
  files: IFiles;
}

export interface DatasetAction {
  type: string;
  data: any;
}

export interface UiAction {
  type: string;
  activeModal?: string;
  search?: string;
}
