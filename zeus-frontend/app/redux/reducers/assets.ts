import { Map, fromJS } from "immutable";

import { INote } from "../../models";
import * as constants from "../constants";
import { ASSETS } from "./data";

export function assetsReducer(
    state: Map<string, any> = fromJS(ASSETS),
    action: any
) {
    switch (action.type) {
        case constants.CREATE_NOTE: {
            const note = <INote> {
                id: action.id,
                created: action.created,
                text: action.text,
            };

            return action.assetIds.reduce((state, id) => state.updateIn([id, "notes"], notes => notes.push(fromJS(note))), state);
        }
            
        default:
            return state;
    }
}

