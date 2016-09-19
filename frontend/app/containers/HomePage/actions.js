import { SEARCH, SEARCH_DONE, LAYER_CREATE, LAYER_TOGGLE_VISIBLE, LAYER_CLOSE, BUCKET_COMPUTATION_START, UI_TOGGLE,
  SELECT_FEATURES, CLEAR_SELECTION,
  NUMERIC_ATTRIBUTES_LOAD, NUMERIC_ATTRIBUTES_LOADED,
  CHART_SELECT_ATTRIBUTE,
  LAYER_SET_STYLE,
  LAYER_TOGGLE_VALUE_VISIBLE,
  MAP_LOADING,
  MAP_LOADED
} from './constants';


export function search(query, callback) {
  console.log("Search started");
  return {
    type: SEARCH,
    query,
    callback
  }
}

export function searchDone(results, callback) {
  return {
    type: SEARCH_DONE,
    response: results,
    callback
  }
}

export function layerCreate(result) {
  return {
    type: LAYER_CREATE,
    id: result.id,
    name: result.name,
    description: result.description,
    stats: result.stats,
    attributeSchema: result.attributeSchema
  }
}

export function layerToggleVisible(layerId) {
  console.assert(layerId != null);
  return {
    type: LAYER_TOGGLE_VISIBLE,
    layerId
  }
}

export function layerClose(layerId) {
  console.assert(layerId != null);
  return {
    type: LAYER_CLOSE,
    layerId
  }
}

export function bucketComputation(computation) {
  console.log("Bucket computation");
  return {
    type: BUCKET_COMPUTATION_START,
    computation: computation
  }
}

export function toggleUi(element) {
  return {
    type: UI_TOGGLE,
    element
  }
}

export function selectFeatures(ids, features) {
  return {
    type: SELECT_FEATURES,
    ids,
    features
  }
}

export function clearSelection() {
  return {
    type: CLEAR_SELECTION
  }
}

export function loadNumericAttributes(layerId) {
  return {
    type: NUMERIC_ATTRIBUTES_LOAD,
    layerId
  }
}

export function loadNumericAttributesDone(data) {
  return {
    type: NUMERIC_ATTRIBUTES_LOADED,
    data
  }
}

export function chartSelectAttribute(attribute) {
  return {
    type: CHART_SELECT_ATTRIBUTE,
    attribute
  }
}

export function setLayerStyle(layerId, style) {
  return {
    type: LAYER_SET_STYLE,
    layerId,
    style
  }
}

export function layerToggleValueVisible(layerId, attribute, value) {
  return {
    type: LAYER_TOGGLE_VALUE_VISIBLE,
    layerId,
    attribute,
    value
  }
}

export function mapLoading() {
  return {
    type: MAP_LOADING
  }
}

export function mapLoaded() {
  return {
    type: MAP_LOADED
  }
}
