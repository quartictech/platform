import { SEARCH, SEARCH_DONE, ITEM_ADD, LAYER_TOGGLE_VISIBLE } from './constants';


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
    name: result.title,
    description: result.description
  }
}

export function toggleLayerVisible(id) {
  return {
    type: LAYER_TOGGLE_VISIBLE,
    id: id
  }
}
