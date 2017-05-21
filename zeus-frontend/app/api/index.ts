import { Asset, DatasetName, Noob } from "../models";
import { ManagedResource } from "../api-management";

export const apiRootUrl = `${location.origin}${location.pathname}api`;

const checkStatus = (response) => {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }
  throw new Error(response.statusText);
};

// NOTE: we may  want to switch representations to seconds since the epoch here for better Java compatibility
const dateInferringReviver = (key, value) => (key.toLowerCase().endsWith("timestamp") ? new Date(value) : value);

const fetchUtil = <T>(url, options?) => fetch(url, Object.assign({}, options, { credentials: "same-origin" }))
  .then(checkStatus)
  .then((response: Response) => response.text() )
  .then<T>(r => JSON.parse(r, dateInferringReviver));

export const assets = <ManagedResource<Map<string, Asset>>>{
  name: "assets",
  shortName: "assets",
  endpoint: (term, limit) =>
    fetchUtil<Map<string, Asset>>(`${apiRootUrl}/datasets/assets`
      + (term ? `?term=${encodeURIComponent(term)}` : "")
      + (limit ? `&limit=${encodeURIComponent(limit)}` : "")),
};

export const asset = <ManagedResource<Asset>>{
  name: "asset",
  shortName: "asset",
  endpoint: (id) => fetchUtil<Asset>(`${apiRootUrl}/datasets/assets/${encodeURIComponent(id)}`)
};

export const datasetList = <ManagedResource<DatasetName[]>>{
  name: "dataset list",
  shortName: "datasetList",
  endpoint: () => fetchUtil(`${apiRootUrl}/datasets`),
};

// TODO: get rid of this
export const noobs = <ManagedResource<{ [id: string] : Noob }>>{
  name: "noobs",
  shortName: "noobs",
  endpoint: (query) =>
    fetchUtil<{ [id: string] : Noob }>(`${apiRootUrl}/datasets/noobs?term=${encodeURIComponent(query)}`),
};
