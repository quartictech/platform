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
  activeModal: string;
}

export interface Profile {
  name: string;
  avatarUrl: string;
}

export interface FeedItem {
  type: string;
  id: string;
  time: number;
}

export interface BuildEvent {
  type: string;
  time: number;
  phase_id: string;
  stream?: string;
}

export interface Build extends FeedItem {
  number: number;
  branch: string;
  status: string;
  trigger: {
    type: string;
  };
  events: BuildEvent[];
}

export interface Validate extends Build { }

export interface Execute extends Build { }

