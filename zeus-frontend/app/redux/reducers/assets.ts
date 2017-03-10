// import * as constants from "../constants";
import { Map, fromJS } from "immutable";

import { ASSETS } from "./data";

const initialState = fromJS(ASSETS);

export function assetsReducer(
    state: Map<string, any> = initialState,
    // action: any
) {
    // TODO
    return state;
}

