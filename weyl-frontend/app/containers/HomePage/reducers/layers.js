import { fromJS, OrderedMap, Set } from "immutable";
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
      return state.mergeIn(["style"], action.style);

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
        .set("attributeSchema", action.schema);

    default:
      return state;
  }
};

const newLayer = (action) => fromJS({
  id: action.id,
  metadata: action.metadata,
  visible: true,
  style: defaultLayerStyle(action.attributeSchema),
  stats: action.stats,
  attributeSchema: action.attributeSchema,
  live: action.live,
  data: {
    type: "FeatureCollection",
    features: [],
  },   // Only relevant in the case of live layers
  filter: defaultFilter(action.attributeSchema),
});

const defaultFilter = (schema) =>
  _.chain(schema.attributes)
    .keys()
    .filter(k => schema.attributes[k].categories !== null)
    .map(k => [k, { notApplicable: false, categories: new Set() }])
    .object()
    .value();

const redTheme = {
  fill: "#67001f",
  line: "#e7298a",
};

const greenTheme = {
  fill: "#00671f",
  line: "#29e78a",
};

const blueTheme = {
  fill: "#001f67",
  line: "#298ae7",
};

const purpleTheme = {
  fill: "#1f0067",
  line: "#8a29e7",
};

const theme = purpleTheme;

const colorScale = "PuRd";
const defaultLayerStyle = schema => ({
  type: "DEFAULT",
  property: schema.primaryAttribute,
  opacity: 0.8,
  point: {
    "circle-radius": 6,
    "color": theme.line,
    colorScale,
  },
  polygon: {
    "color": theme.fill,
    "fill-outline-color": theme.line,
    colorScale,
  },
  line: {
    "color": theme.line,
    colorScale,
  },
});
