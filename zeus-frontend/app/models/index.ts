export interface INote {
  id: number;
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
};

export interface IBar {
  name: string;
  value: number;
};

export interface IInsight {
  id: string;
  title: string;
  body: string;
  insightType: string;
  barChart: IBar[];
  barChartXLabel: string;
}

export interface IAssetInsight extends IInsight {
  insightType: "cluster";
  assetIds: string[];
  assetClass: string;
}

export interface ISmartOpsInsight extends IInsight {
  insightType: "smartops";
}

// TODO: remove this
export interface UiAction {
  type: string;
  activeModal?: string;
  search?: string;
}
