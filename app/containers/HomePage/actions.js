import { SEARCH, SEARCH_DONE, ITEM_ADD, LAYER_TOGGLE_VISIBLE, LAYER_CLOSE, BUCKET_COMPUTATION_START, UI_TOGGLE,
  SELECT_FEATURES, CLEAR_SELECTION,
  NUMERIC_ATTRIBUTES_LOAD, NUMERIC_ATTRIBUTES_LOADED,
  CHART_SELECT_ATTRIBUTE,
  LAYER_SET_STYLE
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

export function addItem(result) {
  return {
    type: ITEM_ADD,
    id: result.id,
    name: result.name,
    description: result.description,
    stats: result.stats
  }
}

export function layerToggleVisible(id) {
  console.assert(id != null);
  return {
    type: LAYER_TOGGLE_VISIBLE,
    id: id
  }
}

export function layerClose(id) {
  console.assert(id != null);
  return {
    type: LAYER_CLOSE,
    id: id
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
