import {
  Asset,
  Insight,
} from "../../models";

const data = require("../../../data/assets.json");
for (const k in data) { // tslint:disable-line:forin
  data[k]["purchaseDate"] = new Date(data[k]["purchaseDate"]);
  data[k]["lastInspectionDate"] = new Date(data[k]["lastInspectionDate"]);
  data[k]["retirementDate"] = new Date(data[k]["retirementDate"]);
  data[k]["events"] = data[k]["events"].map(ev => Object.assign({}, ev, { date: new Date(ev.date)}));
  data[k]["notes"] = data[k]["notes"].map(note => Object.assign({}, note, { created: new Date(note.created)}));
}
export const ASSETS: { [id: string]: Asset } = data;

export const INSIGHTS: Insight[] = require("../../../data/insights.json");
