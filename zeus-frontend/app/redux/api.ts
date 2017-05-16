import { Asset } from "../models";

export const apiRootUrl = `${location.origin}/api`;

const checkStatus = (response) => {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }
  throw new Error(response.statusText);
};

// TODO: this check for 204 is weird
const parseJSON = <T>(response: Response) => ((response.status !== 204) ? <Promise<T>>response.json() : null);

const fetchUtil = <T>(url, options?) => fetch(url, Object.assign({}, options, { credentials: "same-origin" }))
  .then(checkStatus)
  .then<T>(parseJSON);

export const fetchAssets = () => fetchUtil<Map<string, Asset>>(`${apiRootUrl}/assets`);
