import { Intent } from "@blueprintjs/core";
import * as _ from "underscore";
import { SearchProvider, SearchResultEntry } from "./index";
import * as selectors from "../../redux/selectors";
import { appHistory } from "../../routes";
import { stringInString, toTitleCase } from "../../helpers/Utils";
import { toaster } from "../../containers/App/toaster";
import {
  ManagedResource,
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  assets,
  jobs,
} from "../../api";
import {
  Asset,
  Job,
} from "../../models";


function managedResourceProvider<T>(
  selector: (state: any) => ResourceState<{ [id: string] : T }>,
  resource: ManagedResource<{ [id: string] : T }>,
  mapper: (id: string, item: T) => SearchResultEntry
) {
  return (reduxState: any, dispatch: Redux.Dispatch<any>, _onResultChange: () => void) => {
    const resourceState = selector(reduxState);
    return {
      required: (query: string) => (dispatch((query.length > 0)
        ? resourceActions(resource).required(query, 5)
        : resourceActions(resource).clear()
      )),
      result: {
        entries: _.map(resourceState.data, (item, id) => mapper(id, item)),
        loaded: resourceState.status !== ResourceStatus.LOADING,
      },
    };
  };
}

function staticProvider(entries: SearchResultEntry[]) {
  let result = { entries: [], loaded: true };
  return (_reduxState, _dispatch, onResultChange: () => void) => ({
    required: (query: string) => {
      result = {
        entries: (query.length > 0) ? _.filter(entries, e => stringInString(query, e.name)) : [],
        loaded: true,
      };
      onResultChange();
    },
    result,
  });
}

const standardProviders: { [id: string] : SearchProvider } = {
  assets: managedResourceProvider(
    selectors.selectAssets,
    assets,
    (id: string, item: Asset) => ({
      key: id,
      name: toTitleCase(item["Road Name"] || ""),
      description: item["RSL"],
      extra: toTitleCase(item["Section Description"] || ""),
      category: "RSLs",
      iconName: "drive-time", // A car :)
      onSelect: () => appHistory.push(`/assets/${encodeURIComponent(id)}`),
    }),
  ),
  jobs: managedResourceProvider(
    selectors.selectJobs,
    jobs,
    (id: string, item: Job) => ({
      key: id,
      name: toTitleCase(item["Number"] || ""),
      description: item["RSLs"] || "<< No associated RSLs >>",
      extra: item["Type"],
      category: "Jobs",
      iconName: "wrench",
      // TODO: what about the other RSLs?
      onSelect: () =>
        item["RSLs"] && appHistory.push(`/assets/${encodeURIComponent(item["RSLs"].split(",")[0])}`),
    }),
  ),
  gimps: staticProvider(_.map(["Arlo", "Alex", "Oliver"], (name: string) => ({
    key: name,
    name,
    iconName: "person",
    category: "Gimps",
    onSelect: () => toaster.show({ iconName: "person", intent: Intent.SUCCESS, message: name }),
  }))),
};

export default standardProviders;