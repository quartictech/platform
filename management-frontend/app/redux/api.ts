// TODO: Handle contextPaths if we need to
export const apiRootUrl = `${location.origin}/api`;
export const wsUrl = `${location.protocol === "https:" ?
  "wss:" : "ws:"}//${location.host}/ws`;


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

export function fetchDatasets() {
  return fetch(`${apiRootUrl}/dataset`, { credentials: "same-origin" })
    .then(checkStatus)
    .then(parseJSON)
    .then( (data) => ({ data }))
    .catch((err) => ({ err }));
}
