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
// [
//    {
//     id: "I-101",
//     insightType: "cluster",
//     title: "Similar failures occurring in asset class",
//     subInsights: [
//       {icon: "pt-icon-info-sign", text:"Statistically significant increase in failure rate following maintenance work"},
//       {icon: "pt-icon-info-sign", text:"Baseline voltage of circuit diagnostic increased following maintenance activity"},
//     ],
//     assetClass: "Signal",
//     assetIds: _.values(ASSETS).slice(0, 6).map(asset => asset.id),
//     barChart: {
//       data: [
//         { name: "0 -5 years", value: 5 },
//         { name: "5-10 years", value: 5 },
//         { name: "> 10 years", value: 20 },
//       ],
//       xLabel: "# Failures",
//       yLabel: "Asset age"
//     }
//   },
//  {
//     id: "I-103",
//     insightType: "failure",
//     title: "Asset failure predicted",
//     assetIds: [_.last(_.values(ASSETS)).id],
//     assetClass: "Signal",
//     subInsights: [
//       {icon: "pt-icon-info-sign", text: "An increase has been detected in the diagnostic voltage circuit"},
//       {icon: "pt-icon-info-sign", text: "In 35% of previous occurences, this led to a failure within 2 weeks"}
//     ],
//   },
//  {
//     id: "I-102",
//     insightType: "smartops",
//     title: "Jobs taking longer than time estimate",
//     barChart: {
//       data: [
//         { name: "Grass verge maintenance", value: 110 },
//         { name: "Bin emptying", value: 150 },
//         { name: "Pothole maintenance", value: 120 },
//       ],
//       xLabel: "Time taken / estimate (%)",
//       yLabel: "Job Type"
//     },
//     subInsights: [
//       {icon: "pt-icon-info-sign", text: "Some jobs are currently taking longer than estimated."},
//     ],
//   },
//   ];



