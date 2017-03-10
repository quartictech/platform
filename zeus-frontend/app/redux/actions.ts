import * as constants from "./constants";

export function setActiveModal(activeModal) {
  return {
    type: constants.UI_SET_ACTIVE_MODAL,
    activeModal
  };
}

export const createNote = (assetIds: string[], text: string) => ({
  type: constants.CREATE_NOTE,
  assetIds,
  created: new Date(Date.now()),
  text,
});
