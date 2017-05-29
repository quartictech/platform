import * as _ from "underscore";
import { SearchProvider, SearchResultEntry } from "./index";
import * as selectors from "../../redux/selectors";
import { appHistory } from "../../routes";
import { stringInString, toTitleCase } from "../../helpers/Utils";
import {
  ManagedResource,
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  assets,
  jobs,
  datasetList,
} from "../../api";
import {
  Asset,
  Job,
} from "../../models";


const managedResourceProvider = <T>(
  selector: (state: any) => ResourceState<{ [id: string] : T }>,
  resource: ManagedResource<{ [id: string] : T }>,
  mapper: (id: string, item: T) => SearchResultEntry
) => (reduxState: any, dispatch: Redux.Dispatch<any>, _onResultChange: () => void) => {
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

const staticProviderEngine = () => {
  let myQuery = "";
  
  return (entries: SearchResultEntry[], onResultChange: () => void) => ({
    required: (query: string) => {
      myQuery = query;
      onResultChange();
    },
    result: {
      entries: (myQuery.length > 0) ? _.filter(entries, e => stringInString(myQuery, e.name)) : [],
      loaded: true,
    },
  });
}

const staticProvider = (entries: SearchResultEntry[]) => {
  const engine = staticProviderEngine();  // Create upfront so that its state isn't lost every time Redux updates
  return (_reduxState, _dispatch, onResultChange: () => void) => {
    return engine(entries, onResultChange);
  };
}

const getDatasetList = (reduxState: any, dispatch: Redux.Dispatch<any>) => {
  const state = selectors.selectDatasetList(reduxState);
  if (state.status === ResourceStatus.NOT_LOADED) {
    dispatch(resourceActions(datasetList).required());
  }
  return state.data;
}

const datasetProvider = () => {
  const engine = staticProviderEngine();
  return (reduxState: any, dispatch: Redux.Dispatch<any>, onResultChange: () => void) => {
    const datasets = getDatasetList(reduxState, dispatch);
    return engine(_.map(datasets, d => ({
      key: d,
      name: toTitleCase(d),
      iconName: "database",
      category: "Raw data",
      onSelect: () => {},
    })), onResultChange);
  };
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
  people: staticProvider(_.map(["Arlo", "Alex", "Oliver"], p => ({
    key: p,
    name: p,
    category: "People",
    iconName: "person",
    onSelect: () => {},
  }))),
  data: datasetProvider(),
};

export default standardProviders;