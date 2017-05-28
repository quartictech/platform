import * as _ from "underscore";
import { SearchProvider } from "./index";
import { PickerEntry } from "../../components/Picker";
import * as selectors from "../../redux/selectors";
import { appHistory } from "../../routes";
import { toTitleCase } from "../../helpers/Utils";
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


type SearchResourceState<T> = ResourceState<{ [id: string] : T }>;

function managedResourceProvider<T>(
  resource: ManagedResource<{ [id: string] : T }>,
  mapper: (id: string, item: T) => PickerEntry,
  selector: (state: any) => SearchResourceState<T>
) {
  return {
    required: (dispatch: Redux.Dispatch<any>, _fromStore: SearchResourceState<T>) => (query: string) => {
      if (query.length > 0) {
        dispatch(resourceActions(resource).required(query, 5));
      } else {
        dispatch(resourceActions(resource).clear());
      }
    },
    results: (_dispatch: Redux.Dispatch<any>, fromStore: SearchResourceState<T>) =>
        _.map(fromStore.data, (item, id) => mapper(id, item)),
    loaded: (_dispatch: Redux.Dispatch<any>, fromStore: SearchResourceState<T>) =>
        fromStore.status !== ResourceStatus.LOADING,
    selector,
  } as SearchProvider<SearchResourceState<T>>;
};


const assetResults = (id: string, item: Asset) => ({
  key: id,
  name: toTitleCase(item["Road Name"] || ""),
  description: item["RSL"],
  extra: toTitleCase(item["Section Description"] || ""),
  category: "RSLs",
  iconName: "drive-time", // A car :)
  onSelect: () => appHistory.push(`/assets/${encodeURIComponent(id)}`),
}) as PickerEntry;


const jobResults = (id: string, item: Job) => ({
  key: id,
  name: toTitleCase(item["Number"] || ""),
  description: item["RSLs"] || "<< No associated RSLs >>",
  extra: item["Type"],
  category: "Jobs",
  iconName: "wrench",
  // TODO: what about the other RSLs?
  onSelect: () =>
    item["RSLs"] && appHistory.push(`/assets/${encodeURIComponent(item["RSLs"].split(",")[0])}`),
});


const standardProviders: { [id: string] : SearchProvider<any> } = {
  assets: managedResourceProvider(
    assets,
    assetResults,
    selectors.selectAssets
  ),
  jobs: managedResourceProvider(
    jobs,
    jobResults,
    selectors.selectJobs
  ),
};

export default standardProviders;