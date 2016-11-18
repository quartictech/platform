const API_URL = "http://localhost:3010/api";

export function fetchDatasets() {
  return fetch(`${API_URL}/dataset`);
}
