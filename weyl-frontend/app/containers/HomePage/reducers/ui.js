import { fromJS } from "immutable";
import * as constants from "../constants";

const initialState = fromJS({
  layerOp: null,
  panels: {
    chart: false,
    table: false,
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
        case "calculate":
          return state.update("layerOp", val => ((val === element) ? null : element));
        default:
          return state.updateIn(["panels", element], val => !val);
      }
    }
    case constants.UI_SET_THEME:
      return state.setIn(["settings", "theme"], action.theme);
    default:
      return state;
  }
};
