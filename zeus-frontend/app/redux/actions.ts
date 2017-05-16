import { Action } from "redux";
import * as constants from "./constants";
import {
  Asset,
} from "../models";

export function setActiveModal(activeModal) {
  return {
    type: constants.UI_SET_ACTIVE_MODAL,
    activeModal
  };
}

export const createNote = (assetIds: string[], text: string) => ({
  type: constants.CREATE_NOTE,
  id: Math.floor(Math.random() * 100000),
  assetIds,
  created: new Date(Date.now()),
  text,
});


///////////////////////////////////////////////////
// API stuff
///////////////////////////////////////////////////

export interface ApiActionCreators<T> {
  required: () => Action,
  beganLoading: () => Action,
  loaded: (data: T) => Action,
  failedToLoad: () => Action,
};

const createApiActionCreators = <T>(c: constants.ApiConstants) => <ApiActionCreators<T>> { 
  required: () => ({ type: c.required }),
  beganLoading: () => ({ type: c.beganLoading }),
  loaded: (data) => ({ type: c.loaded, data }),
  failedToLoad: () => ({ type: c.failedToLoad }),
};

export const assets = createApiActionCreators<Map<string, Asset>>(constants.ASSETS);