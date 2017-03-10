import { Map, fromJS } from "immutable";

import * as constants from "../constants";
import { ASSETS } from "./data";

export function assetsReducer(
    state: Map<string, any> = fromJS(ASSETS),
    action: any
) {
    switch (action.type) {
        case constants.CREATE_NOTE:
            console.log("CREATE_NOTE", action);
            return state;
        default:
            return state;
    }
}

