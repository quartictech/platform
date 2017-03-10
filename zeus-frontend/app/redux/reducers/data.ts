import * as _ from "underscore";
import {
  IAsset,
  IAssetModel,
  IInsight,
  IAssetInsight,
  ISmartOpsInsight,
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
          { id: 123, created: new Date(2017, 0, 1), text: "What is going on?  I am drowning." },
          { id: 456, created: new Date(2017, 1, 1), text: "I am now dead.  In case you care." },
      ],
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
  <IAssetInsight> {
    id: "I-101",
    insightType: "cluster",
    title: "Repeated failures in asset class",
    body: `<p>
       52% of assets in this class have experienced failures subsequent to recent maintenance interventions.
       </p>

       <p>
         That is noob.
         </p>`,
    assetClass: "wat",
    assetIds: [_.first(_.values(ASSETS)).id]
  },
<ISmartOpsInsight> {
    id: "I-102",
    insightType: "smartops",
    title: "Jobs taking longer than time estimate",
    body: `<p>
        Some jobs are currently taking longer than estimated.
       </p>

       <p>
         That is noob.
         </p>`,
    barChart: [
      {name: "Grass verge maintenance", value: 110},
      {name: "Bin emptying", value: 150},
      {name: "Pothole maintenance", value: 120},
      ],
      
  }
];



