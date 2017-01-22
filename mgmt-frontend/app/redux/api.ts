const apiRootUrl = `${location.origin}/api`;

import { IDatasetMetadata } from "../models";

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
  return fetchUtil(`${apiRootUrl}/dataset`);
}

const validContentType = (t) => (t != null && t.length > 0) ? t : "application/geo+json";

export function uploadFile(files: any[]) {
  return fetchUtil("/api/file", {
    headers: {
      "Content-Type": validContentType(files[0].type),
    },
    method: "POST",
    body: files[0]
  });
}

export function createDataset(metadata: IDatasetMetadata, fileName: string, fileType: string) {
  return fetchUtil("/api/dataset", {
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

export function deleteDataset(id: string) {
  return fetchUtil(`/api/dataset/${id}`, {
    method: "DELETE",
  });
}
