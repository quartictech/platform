const API_URL = "http://localhost:3010/api";

function checkStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }

  const error = new Error(response.statusText);
  throw error;
}

function parseJSON(response) {
  return response.json();
}

export function fetchDatasets(noob) {
  return fetch(`${API_URL}/dataset`)
    .then(checkStatus)
    .then(parseJSON)
    .then( (data) => ({ data }))
    .catch((err) => ({ err }));
}
