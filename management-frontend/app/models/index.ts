
export interface IDatasetMetadata {
  name: string;
  description: string;
  attribution: string;
  icon: string;
}

export interface IDatasetLocator {
  type: string;
}

export interface IDataset {
  metadata: IDatasetMetadata;
  locator: IDatasetLocator;
}

export interface DatasetAction {
  type: string;
  data: any;
}
