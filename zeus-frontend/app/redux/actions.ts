import * as constants from "./constants";

export function setActiveModal(activeModal) {
  return {
    type: constants.UI_SET_ACTIVE_MODAL,
    activeModal
  };
}
