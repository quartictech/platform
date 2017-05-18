import { Asset } from "../models";
import { ManagedResource } from "../api-management";

export const apiRootUrl = `${location.origin}${location.pathname}api`;

const checkStatus = (response) => {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }
  throw new Error(response.statusText);
};

const dateInferringReviver = (key, value) => (key.toLowerCase().endsWith("timestamp") ? new Date(value * 1000) : value);

const fetchUtil = <T>(url, options?) => fetch(url, Object.assign({}, options, { credentials: "same-origin" }))
  .then(checkStatus)
  .then((response: Response) => response.text() )
  .then<T>(r => JSON.parse(r, dateInferringReviver));

export const assets = <ManagedResource<Map<string, Asset>>>{
  name: "assets",
  shortName: "assets",
  endpoint: () => fetchUtil<Map<string, Asset>>(`${apiRootUrl}/datasets/assets`),
};

export const asset = <ManagedResource<Asset>>{
  name: "asset",
  shortName: "asset",
  endpoint: (id) => fetchUtil<Asset>(`${apiRootUrl}/datasets/assets/${id}`)
};
