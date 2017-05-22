export interface Note {
  id: string;
  timestamp: Date;
  text: string;
}

export interface LatLon {
  lat: number;
  lon: number;
}

export type DatasetName = string;

export type Asset = any;

export type Job = any;

export interface Bar {
  name: string;
  value: number;
}

export interface SubInsight {
  icon: string;
  text: string;
}

export interface Insight {
  id: string;
  title: string;
  insightType: string;
  barChart?: {
    data: Bar[];
    xLabel: string;
    yLabel: string;
  };
  assetIds?: string[];
  unfailedAssetIds?: string[];
  subInsights: SubInsight[];
  assetClass?: string;
}

// TODO: remove this
export interface UiAction {
  type: string;
  activeModal?: string;
  search?: string;
}

export interface TimeSeriesPoint {
  x: Date;
  y: number;
}

export interface MaintenanceEvent {
  type: string;
  timestamp: Date;
  detail: string;
}

