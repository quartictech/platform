import { fromJS, OrderedMap, Set } from "immutable";
import { layerThemes } from "../../../themes";
import * as constants from "../constants";
const _ = require("underscore");

export default (state = new OrderedMap(), action) => {
  switch (action.type) {
    case constants.LAYER_CREATE:
      return (state.has(action.id)) ? state : state.set(action.id, newLayer(action));
    case constants.LAYER_CLOSE:
      return state.delete(action.layerId);
    case constants.LAYER_TOGGLE_VISIBLE:
    case constants.LAYER_SET_STYLE:
    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
    case constants.LAYER_SET_DATA:
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
      if (action.value === undefined) {
        return state.updateIn(["filter", action.attribute, "notApplicable"], na => !na);
      }
      return state.updateIn(["filter", action.attribute, "categories"], set => {
        if (set.includes(action.value)) {
          return set.remove(action.value);
        }
        return set.add(action.value);
      });

    case constants.LAYER_SET_DATA:
      return state
        .set("data", action.data)
        .set("schema", action.schema);

    default:
      return state;
  }
};

const newLayer = (action) => fromJS({
  id: action.id,
  metadata: action.metadata,
  visible: true,
  themeIdx: 0,
  style: defaultLayerStyle(action.schema.primaryAttribute, 0),
  stats: action.stats,
  schema: action.schema,
  live: action.live,
  data: {
    type: "FeatureCollection",
    features: [],
  },   // Only relevant in the case of live layers
  filter: defaultFilter(action.schema),
});

const defaultFilter = (schema) =>
  _.chain(schema.attributes)
    .keys()
    .filter(k => schema.attributes[k].categories !== null)
    .map(k => [k, { notApplicable: false, categories: new Set() }])
    .object()
    .value();

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
