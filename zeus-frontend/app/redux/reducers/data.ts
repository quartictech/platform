import * as _ from "underscore";
import {
  IAsset,
  IAssetModel,
  IInsight,
  IIncidentClusterInsight,
  ISmartOpsInsight,
  IFailureInsight
} from "../../models";

const ENGINEERS = ["J Nole", "A McFadden", "G Kemp", "B Wilson", "B Gee", "P Graham"];

const SN_GEN_SIEM = () => Math.random().toString(36).slice(2, 10);
const SN_GEN_GE = () => Math.random().toString(10).slice(2, 12);

const MODELS: IAssetModel[] = [
  { name: "S-5000C", snGen: SN_GEN_SIEM, manufacturer: "SIEM" },
  { name: "S-5000B", snGen: SN_GEN_SIEM, manufacturer: "SIEM" },
  { name: "QQ-19", snGen: SN_GEN_GE, manufacturer: "GE" },
  { name: "QQ-23", snGen: SN_GEN_GE, manufacturer: "GE" },
];

function generateAssets() {
  var assets: { [id: string]: IAsset } = {};
  for (var i = 0; i < 50; i++) {
    const model = MODELS[Math.floor(Math.random() * MODELS.length)]; 
    const id = "AB" + (Math.floor(Math.random() * 90000) + 10000);

    assets[id] = {
      id,
      clazz: "Boiler",
      model: model,
      serial: model.snGen(),
      purchaseDate: randomDate(new Date(2003, 0, 1), new Date(2013, 0, 1)),
      lastInspectionDate: randomDate(new Date(2016, 0, 1), new Date(2017, 0, 1)),
      lastInspectionSignoff: ENGINEERS[Math.floor(Math.random() * 6)],
      retirementDate: randomDate(new Date(2018, 0, 1), new Date(2020, 0, 1)),
      location: randomLocation(),
      notes: [
          { id: "123", created: new Date(2017, 0, 1), text: "What is going on?  I am drowning." },
          { id: "456", created: new Date(2017, 1, 1), text: "I am now dead.  In case you care." },
      ],
      events: [
        { type: "maintenance", date: new Date(2016, 9, 10) }, 
        { type: "maintenance", date: new Date(2017, 1, 3) },
        { type: "failure", date: new Date(2017, 2, 2) }
      ]
    };
  }
  return assets;
}

function randomLocation() {
  return (Math.random() * (58.64 - 50.83) + 50.83).toFixed(3) + ", " + (Math.random() * (1.32 - -5.37) + -5.37).toFixed(3)
}

function randomDate(start: Date, end: Date) {
  return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
}

export const ASSETS: { [id: string]: IAsset } = generateAssets();

export const INSIGHTS: IInsight[] = [


  <IIncidentClusterInsight> {
    id: "I-101",
    insightType: "cluster",
    title: "Similar failures occurring in asset class",
    body: "60% of assets in this class have experienced failures within 4 weeks of maintenance intervention",
    subInsights: [
      {icon: "pt-icon-info-sign", text:"Statistically significant increase in failure rate following maintenance work"},
      {icon: "pt-icon-info-sign", text:"Baseline voltage of circuit diagnostic increased following maintenance activity"},
    ],
    assetClass: "train-sensor-x100",
    assetIds: _.values(ASSETS).slice(0, 6).map(asset => asset.id),
    barChart: [
      {name: "0 -5 years", value: 5},
      {name: "5-10 years", value: 5},
      {name: "> 10 years", value: 20},
    ],
    barChartXLabel: "# Failures"
  },

<IFailureInsight> {
    id: "I-103",
    insightType: "failure",
    title: "Asset likely to experience failure soon",
    body: ""
  },
<ISmartOpsInsight> {
    id: "I-102",
    insightType: "smartops",
    title: "Jobs taking longer than time estimate",
    body: `<p>
        Some jobs are currently taking longer than estimated.
       </p>`,

    barChart: [
      {name: "Grass verge maintenance", value: 110},
      {name: "Bin emptying", value: 150},
      {name: "Pothole maintenance", value: 120},
      ],
    barChartXLabel: "Time taken / estimate (%)",
  },
  ];



