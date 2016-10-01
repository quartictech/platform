import { fromJS, Set } from "immutable";
import * as constants from "../constants";

const colorScale = "PuRd";
const defaultLayerStyle = schema => ({
  type: "DEFAULT",
  property: schema.primaryAttribute,
  opacity: 0.8,
  point: {
    "circle-radius": 6,
    "color": "#e7298a", // "#223b53",
    colorScale,
  },
  polygon: {
    "color": "#67001f", // #F2F12D",
    "fill-outline-color": "#E0A025",
    colorScale,
  },
  line: {
    "color": "#e7298a",
    // color: "#E0A025",
    colorScale,
  },
});

const newLayer = (action) => fromJS({
  id: action.id,
  name: action.name,
  description: action.description,
  visible: true,
  closed: false,
  style: defaultLayerStyle(action.attributeSchema),
  stats: action.stats,
  attributeSchema: action.attributeSchema,
  live: action.live,
  data: {
    type: "FeatureCollection",
    features: [],
  },   // Only relevant in the case of live layers
  filter: {},
});

const layerReducer = (state, action) => {
  switch (action.type) {
    case constants.LAYER_TOGGLE_VISIBLE:
      return state.set("visible", !layerState.get("visible"));
    case constants.LAYER_CLOSE:
      return state.set("visible", false).set("closed", true);
    case constants.LAYER_SET_STYLE:
      return state.mergeIn(["style"], action.style);
    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
      return state.updateIn(["filter", action.attribute], new Set(), set => {
        if (set.has(action.value)) {
          return set.remove(action.value);
        }
        return set.add(action.value);
      });
    case constants.LAYER_SET_DATA:
      return state.set("data", action.data);
    default:
      return state;
  }
};

export default (state = List(), action) => {
  switch (action.type) {
    case constants.LAYER_CREATE:
      return state.push(newLayer(action));
    case constants.LAYER_TOGGLE_VISIBLE:
    case constants.LAYER_CLOSE:
    case constants.LAYER_SET_STYLE:
    case constants.LAYER_TOGGLE_VALUE_VISIBLE:
    case constants.LAYER_SET_DATA:
      const idx = state.findKey(layer => layer.get("id") === action.layerId);
      const val = state.get(idx);
      return state.set(idx, layerReducer(val, action));
    default:
      return state;
  }
};
