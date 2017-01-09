import { fromJS, OrderedMap, Set } from "immutable";
import { layerThemes } from "../../../themes";
import * as constants from "../constants";

export default (state = new OrderedMap(), action) => {
  switch (action.type) {
    case constants.LAYER_CREATE:
      return (state.has(action.layerId)) ? state : state.set(action.layerId, newLayer(action));
    case constants.LAYER_CLOSE:
      return state.delete(action.layerId);
    case constants.LAYER_TOGGLE_VISIBLE:
    case constants.LAYER_SET_STYLE:
    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
    case constants.LAYER_UPDATE:
      return state.update(action.layerId, val => layerReducer(val, action));
    default:
      return state;
  }
};

const layerReducer = (state, action) => {
  switch (action.type) {
    case constants.LAYER_TOGGLE_VISIBLE:
      return state.set("visible", !state.get("visible"));

    case constants.LAYER_SET_STYLE:
      switch (action.key) {
        case "ATTRIBUTE":
          return state.setIn(["style", "attribute"], action.value);
        case "THEME":
          return state
            .set("themeIdx", action.value)
            .set("style", fromJS(defaultLayerStyle(state.getIn(["style", "attribute"]), action.value)));
        default:
          console.error("Unknown style key", action.key);
          return state;
      }

    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
      return state.updateIn(["filter", action.attribute], defaultAttributeFilter(), filter => {
        if (action.value === undefined) {
          return filter.update("notApplicable", x => !x);
        }
        return filter.update("categories", set => {
          if (set.includes(action.value)) {
            return set.remove(action.value);
          }
          return set.add(action.value);
        });
      });

    case constants.LAYER_UPDATE:
      return state
        .set("data", action.data)
        .set("stats", action.stats)
        .set("dynamicSchema", action.dynamicSchema);

    default:
      return state;
  }
};

const newLayer = (action) => fromJS({
  id: action.layerId,
  visible: true,
  themeIdx: 0,
  style: defaultLayerStyle(null, 0),
  stats: {
    attributeStats: {},
  },
  dynamicSchema: {
    attributes: {},
  },
  data: {
    type: "FeatureCollection",
    features: [],
  },   // Only relevant in the case of live layers
  filter: {},
});

const defaultAttributeFilter = () => fromJS({
  notApplicable: false,
  categories: new Set(),
});

const defaultLayerStyle = (attribute, themeIdx) => ({
  type: "DEFAULT",
  attribute,
  opacity: 0.8,
  point: {
    "circle-radius": 6,
    "color": layerThemes[themeIdx].line,
    "colorScale": layerThemes[themeIdx].colorScale,
  },
  polygon: {
    "color": layerThemes[themeIdx].fill,
    "fill-outline-color": layerThemes[themeIdx].line,
    "colorScale": layerThemes[themeIdx].colorScale,
  },
  line: {
    "color": layerThemes[themeIdx].line,
    "colorScale": layerThemes[themeIdx].colorScale,
  },
});
