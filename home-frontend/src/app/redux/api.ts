export const apiRootUrl = `${location.origin}/api`;

import { IDatasetMetadata, IDatasetCoords } from "../models";

import { QUARTIC_XSRF, QUARTIC_XSRF_HEADER } from "../helpers/Utils";

export class ApiError extends Error {
  status: number;
  constructor(m: string, status: number) {
    super(m);
    // apparently necessary in TS. See: https://github.com/Microsoft/TypeScript-wiki/blob/master/Breaking-Changes.md
    Object.setPrototypeOf(this, ApiError.prototype);
    this.status = status;
  }
}

function checkStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }

  const error = new ApiError(response.statusText, response.status);
  throw error;
}

function parseJSON(response: Response) {
  if (response.status !== 204) {
    return response.json();
  }
  return null;
}

export function fetchUtil(url, options?) {
  const headers = options && options.headers ? options.headers : {};
  headers[QUARTIC_XSRF_HEADER] = localStorage.getItem(QUARTIC_XSRF);
  headers["Accept"] = "application/json";
  const newOptions = Object.assign({}, options, {
    credentials: "same-origin",
    headers,
  });
  return fetch(url, newOptions)
    .then(checkStatus)
    .then(parseJSON)
    .then(data => ({ data }))
    .catch(err => ({ err }));
}

export function fetchAuth(url, options?) {
  const newOptions = Object.assign({}, options, { credentials: "same-origin" });
  return fetch(url, newOptions)
    .then(checkStatus)
    .then(r => ({ xsrfToken: r.headers.get(QUARTIC_XSRF_HEADER) }))
    .catch(err => ({ err }));
}

export function fetchDatasets() {
  return fetchUtil(`${apiRootUrl}/datasets`);
}

export function fetchDag(build: string) {
  if (build != null) {
    return fetchUtil(`${apiRootUrl}/dag/${build}`);
  }
  return fetchUtil(`${apiRootUrl}/dag/`);
}

export function githubAuth(code: string, state: string) {
  return fetchAuth(
    `${apiRootUrl}/auth/gh/complete?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`,
    {
      method: "POST",
    },
  );
}

export function uploadFile(files: any[]) {
  return fetchUtil(`${apiRootUrl}/file`, {
    method: "POST",
    body: files[0],
  });
}

export function buildPipeline() {
  return fetchUtil(`${apiRootUrl}/build`, {
    method: "POST",
  });
}

export function createDataset(metadata: IDatasetMetadata, fileName: string) {
  return fetchUtil(`${apiRootUrl}/datasets`, {
    headers: {
      "Content-Type": "application/json",
    },
    method: "POST",
    body: JSON.stringify({
      metadata,
      file_name: fileName,
    }),
  });
}

export function deleteDataset(coords: IDatasetCoords) {
  return fetchUtil(`${apiRootUrl}/datasets/${encodeURIComponent(coords.id)}`, {
    method: "DELETE",
  });
}
