import { SEARCH, SEARCH_DONE, ITEM_ADD, LAYER_TOGGLE_VISIBLE, BUCKET_COMPUTATION_START } from './constants';


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

export function bucketComputation(computation) {
  console.log("Bucket computation");
  return {
    type: BUCKET_COMPUTATION_START,
    computation: computation
  }
}
