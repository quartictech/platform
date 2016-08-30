import { SEARCH, SEARCH_DONE, ITEM_ADD } from './constants';


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
  console.log("Add item");
  console.log(result);
  return {
    type: ITEM_ADD,
    id: result.id
  }
}
