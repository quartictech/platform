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

export interface PipelineAction {
  type: string;
  data: any;
}

export interface ProfileAction {
  type: string;
  data: any;
}

export interface UiAction {
  type: string;
  activeModal?: string;
  search?: string;
  namespace?: string;
}

export interface Ui {
  namespace: string;
  searchString: string;
  activeModal: string;
}

export interface Profile {
  name: string;
  avatarUrl: string;
}

export interface Build {
  type: string;
  id: string;
  buildNumber: number;
  branch: string;
  status: string;
  triggerDetails: {
    repoFullName: string;
  }
}

export interface Other {
  type: string;
  id: string;
}



export type FeedItem = Build | Other
