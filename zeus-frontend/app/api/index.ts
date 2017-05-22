import { Asset, Job, DatasetName } from "../models";
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

const searchableResource = <T>(name: string) => ({
  name,
  shortName: name,
  endpoint: (term, limit) =>
    fetchUtil<Map<string, T>>(`${apiRootUrl}/datasets/${name}`
      + (term ? `?term=${encodeURIComponent(term)}` : "")
      + (limit ? `&limit=${encodeURIComponent(limit)}` : "")),
});

export const jobs = searchableResource<Job>("jobs");
export const assets = searchableResource<Asset>("assets");

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

export const datasetContent = <ManagedResource<{ [id: string] : any }>>{
  name: "dataset content",
  shortName: "datasetContent",
  endpoint: (dataset: DatasetName) => fetchUtil(`${apiRootUrl}/datasets/${encodeURIComponent(dataset)}`),
};

export const managedResources: ManagedResource<any>[] = [
  jobs,
  assets,
  asset,
  datasetList,
  datasetContent,
];
