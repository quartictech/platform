import {
  IAsset,
  IInsight,
} from "../../models";

const data = require("../../../data/assets.json");
for (const k in data) {
  data[k]["purchaseDate"] = new Date(data[k]["purchaseDate"]);
  data[k]["lastInspectionDate"] = new Date(data[k]["lastInspectionDate"]);
  data[k]["retirementDate"] = new Date(data[k]["retirementDate"]);
  data[k]["events"] = data[k]["events"].map(ev => Object.assign({}, ev, { date: new Date(ev.date)}));
  data[k]["notes"] = data[k]["notes"].map(note => Object.assign({}, note, { created: new Date(note.created)}));
}
console.log(data);
export const ASSETS: { [id: string]: IAsset } = data;

export const INSIGHTS: IInsight[] = require("../../../data/insights.json");
