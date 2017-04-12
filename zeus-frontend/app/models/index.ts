export interface INote {
  id: string;
  created: Date;
  text: string;
}

export interface IAssetModel {
  name: string,
  manufacturer: string,
  snGen: () => string
};

export interface ILatLon {
  lat: number;
  lon: number;
};

export interface IAsset {
  id: string;
  clazz: string;
  model: IAssetModel;
  serial: string;
  purchaseDate: Date;
  lastInspectionDate: Date;
  lastInspectionSignoff: string;
  retirementDate: Date;
  location: ILatLon;
  notes: INote[];
  events: IMaintenanceEvent[];
};

export interface IBar {
  name: string;
  value: number;
};

export interface ISubInsight {
  icon: string;
  text: string;
}

export interface IInsight {
  id: string;
  title: string;
  insightType: string;
  barChart?: {
    data: IBar[];
    xLabel: string;
    yLabel: string;
  },
  assetIds?: string[];
  unfailedAssetIds?: string[];
  subInsights: ISubInsight[];
  assetClass?: string;
}

// TODO: remove this
export interface UiAction {
  type: string;
  activeModal?: string;
  search?: string;
}

export interface ITimeSeriesPoint {
  x: Date;
  y: number;
}

export interface IMaintenanceEvent {
  type: string;
  date: Date;
}


