export interface Note {
  id: string;
  timestamp: Date;
  text: string;
}

export interface LatLon {
  lat: number;
  lon: number;
}

export interface Dataset<T> {
  schema: string[];
  content: { [id: string] : T };
}

export interface DatasetInfo {
  prettyName: string;
}

export interface SessionInfo {
  username: string;
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

export type Insight = any;

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

