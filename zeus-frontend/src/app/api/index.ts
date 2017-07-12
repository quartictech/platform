import { Asset, Job, Dataset, DatasetInfo, DatasetName, SessionInfo, Insight } from "../models";
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

const fetchUtil = <T>(url, options?) => fetch(
  `${apiRootUrl}${url}`, Object.assign({}, options, { credentials: "same-origin" }))
  .then(checkStatus)
  .then((response: Response) => response.text() )
  .then<T>(r => JSON.parse(r, dateInferringReviver));

const searchableResource = <T>(name: string) => ({
  name,
  shortName: name,
  endpoint: (term, limit) =>
    fetchUtil<Dataset<T>>(`/datasets/${name}`
      + (term ? `?term=${encodeURIComponent(term)}` : "")
      + (limit ? `&limit=${encodeURIComponent(limit)}` : "")),
});

export const jobs = searchableResource<Job>("jobs");
export const assets = searchableResource<Asset>("assets");

export const sessionInfo = <ManagedResource<SessionInfo>>{
  name: "Session info",
  shortName: "sessionInfo",
  endpoint: () => fetchUtil<SessionInfo>(`/session-info`),
};

export const asset = <ManagedResource<Asset>>{
  name: "asset",
  shortName: "asset",
  endpoint: (id) => fetchUtil(`/datasets/assets/${encodeURIComponent(id)}`),
};

export const insight = <ManagedResource<Insight>>{
  name: "insight",
  shortName: "insight",
  endpoint: (insight) => fetchUtil(`/datasets/insights/${encodeURIComponent(insight)}`),
};

export const datasetInfo = <ManagedResource<{ [id: string] : DatasetInfo }>>{
  name: "dataset info",
  shortName: "datasetInfo",
  endpoint: () => fetchUtil(`/datasets`),
};

export const datasetContent = <ManagedResource<Dataset<any>>>{
  name: "dataset content",
  shortName: "datasetContent",
  endpoint: (dataset: DatasetName) => fetchUtil(`/datasets/${encodeURIComponent(dataset)}`),
};

export const downloadLinkFor = (dataset: DatasetName) => `${apiRootUrl}/datasets/csv/${encodeURIComponent(dataset)}`;

export const managedResources: ManagedResource<any>[] = [
  sessionInfo,
  jobs,
  assets,
  asset,
  insight,
  datasetInfo,
  datasetContent,
];
