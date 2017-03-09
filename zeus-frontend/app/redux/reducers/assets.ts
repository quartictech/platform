import { IAsset, IAssetModel } from "../../models";
// import * as constants from "../constants";
import { Map, fromJS } from "immutable";

const ENGINEERS = ["J Nole", "A McFadden", "G Kemp", "B Wilson", "B Gee", "P Graham"];

const MODELS: IAssetModel[] = [
  { name: "S-5000C", snGen: () => Math.random().toString(36).slice(2, 10), manufacturer: "SIEM" },
  { name: "S-5000B", snGen: () => Math.random().toString(36).slice(2, 10), manufacturer: "SIEM" },
  { name: "QQ-19", snGen: () => Math.random().toString(10).slice(2, 12), manufacturer: "GE" },
  { name: "QQ-23", snGen: () => Math.random().toString(10).slice(2, 12), manufacturer: "GE" },
];

const initialState = fromJS(generateAssets());

export function assetsReducer(
    state: Map<string, any> = initialState,
    // action: any
) {
    // TODO
    return state;
}

function generateAssets() {
  var assets = new Array<IAsset>();
  for (var i = 0; i < 50; i++) {
    const model = MODELS[Math.floor(Math.random() * MODELS.length)]; 

    assets.push({
      id: "AB" + (Math.floor(Math.random() * 90000) + 10000),
      clazz: "Boiler",
      model: model,
      serial: model.snGen(),
      purchaseDate: randomDate(new Date(2003, 0, 1), new Date(2013, 0, 1)),
      lastInspectionDate: randomDate(new Date(2016, 0, 1), new Date(2017, 0, 1)),
      lastInspectionSignoff: ENGINEERS[Math.floor(Math.random() * 6)],
      retirementDate: randomDate(new Date(2018, 0, 1), new Date(2020, 0, 1)),
      location: randomLocation(),
      notes: [],
    });
  }
  return assets;
}

function randomLocation() {
  return (Math.random() * (58.64 - 50.83) + 50.83).toFixed(3) + ", " + (Math.random() * (1.32 - -5.37) + -5.37).toFixed(3)
}

function randomDate(start: Date, end: Date) {
  return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
}

