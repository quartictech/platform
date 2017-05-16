import { Asset } from "../models";
import { ManagedResource } from "../api-management";

export const apiRootUrl = `${location.origin}/api`;

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
  endpoint: () => fetchUtil<Map<string, Asset>>(`${apiRootUrl}/assets`),
};
