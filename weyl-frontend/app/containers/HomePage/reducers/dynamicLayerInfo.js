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
    case constants.LAYER_TOGGLE_ALL_VALUES_VISIBLE:
    case constants.LAYER_APPLY_TIME_RANGE_FILTER:
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
        case "TRANSPARENCY":
          return state.setIn(["style", "isTransparent"], action.value);
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

    case constants.LAYER_TOGGLE_ALL_VALUES_VISIBLE:
      return state.updateIn(["filter", action.attribute], defaultAttributeFilter(), filter =>
        filter.update("notApplicable", x => !x)
          .update("categories", set => new Set(state.getIn(["dynamicSchema", "attributes", action.attribute, "categories"])).subtract(set))
    );

    case constants.LAYER_APPLY_TIME_RANGE_FILTER:
      return state.updateIn(["filter", action.attribute], defaultAttributeFilter(), filter => {
        if (action.startTime != null || action.endTime != null) {
          return filter.set("timeRange", fromJS({ startTime: action.startTime, endTime: action.endTime }));
        }
        return filter.set("timeRange", null);
      });

    case constants.LAYER_UPDATE:
      return state
        .set("snapshotId", action.snapshotId)
        .set("data", action.data)
        .set("stats", fromJS(action.stats))
        .set("dynamicSchema", fromJS(action.dynamicSchema));

    default:
      return state;
  }
};

const newLayer = (action) => fromJS({
  id: action.layerId,
  snapshotId: 0,    // Technically, this doesn't match any SnapshotID on the backend, but that doesn't currently matter
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
  timeRange: null,
});

const defaultLayerStyle = (attribute, themeIdx) => ({
  type: "DEFAULT",
  attribute,
  opacity: 0.8,
  isTransparent: false,
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
