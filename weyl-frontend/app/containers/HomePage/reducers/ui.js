import { fromJS } from "immutable";
import * as constants from "../constants";
import { mapThemes } from "../../../themes";

const initialState = fromJS({
  layerOp: null,
  panels: {
    chart: false,
    layerList: true,
    settings: false,
  },
  settings: {
    theme: "dark",
  },
});

export default (state = initialState, action) => {
  switch (action.type) {
    case constants.UI_TOGGLE: {
      const element = action.element;
      switch (element) {
        case "bucket":
        case "geofence":
          return state.update("layerOp", val => ((val === element) ? null : element));
        case "theme":
          return state.updateIn(["settings", "theme"], val => mapThemes[val].next);
        default:
          return state.updateIn(["panels", element], val => !val);
      }
    }
    default:
      return state;
  }
};
