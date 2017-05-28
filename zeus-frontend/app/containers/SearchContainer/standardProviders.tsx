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


function managedResourceProvider<T>(
  resource: ManagedResource<{ [id: string] : T }>,
  mapper: (id: string, item: T) => PickerEntry,
  selector: (state: any) => ResourceState<{ [id: string] : T }>
) {
  return (reduxState: any) => {
    const resourceState = selector(reduxState);
    return (dispatch: Redux.Dispatch<any>) => ({
      required: (query: string) => {
        if (query.length > 0) {
            dispatch(resourceActions(resource).required(query, 5));
        } else {
            dispatch(resourceActions(resource).clear());
        }
      },
      results: _.map(resourceState.data, (item, id) => mapper(id, item)),
      loaded: resourceState.status !== ResourceStatus.LOADING,
    });
  };
}


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


const standardProviders: { [id: string] : SearchProvider } = {
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