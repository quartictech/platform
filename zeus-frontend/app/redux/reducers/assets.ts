import { Map, fromJS } from "immutable";

import { Note } from "../../models";
import * as constants from "../constants";
import { ASSETS } from "./data";

export function assetsReducer(
  state: Map<string, any> = fromJS(ASSETS),
  action: any
) {
  switch (action.type) {
    case constants.ASSETS.beganLoading:
      console.log("Loading...");
      return state;

    case constants.ASSETS.failedToLoad:
      console.log("Failed to load :(");
      return state;

    case constants.ASSETS.loaded:
      console.log("Loaded :)", action.data);
      return state;

    case constants.CREATE_NOTE:
      return action.assetIds.reduce((state, id) => state.updateIn([id, "notes"],
        notes => notes.push(fromJS(<Note> {
            id: action.id,
            created: action.created,
            text: action.text,
        }))), state);

    default:
      return state;
  }
}

