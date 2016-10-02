import { fromJS } from "immutable";
import * as constants from "../constants";
import { themes } from "../../../themes";

const initialState = fromJS({
  layerOp: null,
  panels: {
    chart: false,
    layerList: true,
    settings: false,
    liveFeed: false,
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
          return state.updateIn(["settings", "theme"], val => themes[val].next);
        default:
          return state.updateIn(["panels", element], val => !val);
      }
    }
    default:
      return state;
  }
};
