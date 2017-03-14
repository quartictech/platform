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

export interface IAsset {
  id: string;
  clazz: string;
  model: IAssetModel;
  serial: string;
  purchaseDate: Date;
  lastInspectionDate: Date;
  lastInspectionSignoff: string;
  retirementDate: Date;
  location: string;
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
  body: string;
  insightType: string;
  barChart: IBar[];
  assetIds: string[];
  barChartXLabel: string;
  subInsights: ISubInsight[];
}

export interface IIncidentClusterInsight extends IInsight {
  insightType: "cluster";
  assetClass: string;
}

export interface ISmartOpsInsight extends IInsight {
  insightType: "smartops";
}

export interface IFailureInsight extends IInsight {
  insightType: "failure";
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


