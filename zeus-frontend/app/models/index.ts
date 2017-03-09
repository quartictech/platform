
export interface IAssetModel {
  name: string,
  manufacturer: string,
  snGen: () => string
};

export interface IAsset {
  id: string;
  clazz: string;
  model: string;
  serial: string;
  manufacturer: string;
  purchaseDate: Date;
  lastInspectionDate: Date;
  lastInspectionSignoff: string;
  retirementDate: Date;
  location: string;
};

// TODO: remove this
export interface UiAction {
  type: string;
  activeModal?: string;
  search?: string;
}
