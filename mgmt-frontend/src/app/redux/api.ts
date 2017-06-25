const apiRootUrl = `${location.origin}${location.pathname}api`;

import { IDatasetMetadata, IDatasetCoords } from "../models";

function checkStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }

  const error = new Error(response.statusText);
  throw error;
}

function parseJSON(response: Response) {
  if (response.status !== 204) {
    return response.json();
  }
  return null;
}

export function fetchUtil(url, options?) {
  const newOptions = Object.assign({}, options, { credentials: "same-origin" });
  return fetch(url, newOptions)
    .then(checkStatus)
    .then(parseJSON)
    .then((data) => ({ data }))
    .catch((err) => ({ err }));
}

export function fetchDatasets() {
  return fetchUtil(`${apiRootUrl}/datasets`);
}

const validContentType = (t) => (t != null && t.length > 0) ? t : "application/geo+json";

export function uploadFile(namespace: string, files: any[]) {
  return fetchUtil(`${apiRootUrl}/file/${encodeURIComponent(namespace)}`, {
    headers: {
      "Content-Type": validContentType(files[0].type),
    },
    method: "POST",
    body: files[0]
  });
}

// TODO: wire through namespace
export function createDataset(namespace: string, metadata: IDatasetMetadata, fileName: string, fileType: string) {
  return fetchUtil(`${apiRootUrl}/datasets/${encodeURIComponent(namespace)}`, {
    headers: {
      "Content-Type": "application/json"
    },
    method: "POST",
    body: JSON.stringify({
      type: "static",
      metadata,
      fileName,
      fileType
    })
  });
}

// TODO: wire through namespace
export function deleteDataset(coords: IDatasetCoords) {
  return fetchUtil(`${apiRootUrl}/datasets/${encodeURIComponent(coords.namespace)}/${encodeURIComponent(coords.id)}`, {
    method: "DELETE",
  });
}