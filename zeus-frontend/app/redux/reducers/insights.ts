import { IInsight, IAssetInsight } from "../../models";
// import * as constants from "../constants";
import {  fromJS } from "immutable";

const INSIGHTS: IInsight[]  = [
  <IAssetInsight> {
    id: "I-101",
    title: "Repeated failures in asset class",
    body: `<p>
       52% of assets in this class have experienced failures subsequent to recent maintenance interventions.
       </p>

       <p>
         That is noob.
         </p>`,
    assetClass: "wat",
    assetIds: [0]
  }
]

const initialState = fromJS(INSIGHTS);

export function insightsReducer(
    state: IInsight[] = initialState,
    // action: any
) {
    // TODO
    return state;
}