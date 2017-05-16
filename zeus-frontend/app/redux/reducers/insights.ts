import { Insight } from "../../models";
// import * as constants from "../constants";
import {  fromJS } from "immutable";
import { INSIGHTS } from "./data";


const initialState = fromJS(INSIGHTS);

export function insightsReducer(
    state: Insight[] = initialState,
    // action: any
) {
    // TODO
    return state;
}
