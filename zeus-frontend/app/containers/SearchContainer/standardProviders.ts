import * as _ from "underscore";
import { SearchProvider } from "./index";
import * as selectors from "../../redux/selectors";
import { toTitleCase } from "../../helpers/Utils";
import { Intent } from "@blueprintjs/core";
import { toaster } from "../../containers/App/toaster";
import {
  resourceActions,
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
import { managedResourceProvider } from "./managedResourceProvider";
import { staticProvider, staticProviderEngine } from "./staticProvider";
import insights from "../../containers/InsightView/insights";


const getDatasetList = (reduxState: any, dispatch: Redux.Dispatch<any>) => {
  const state = selectors.selectDatasetList(reduxState);
  if (state.status === ResourceStatus.NOT_LOADED) {
    dispatch(resourceActions(datasetList).required());
  }
  return state.data;
};

const datasetProvider = () => {
  const engine = staticProviderEngine(["datasets", "raw", "explorer"]);
  return (reduxState: any, dispatch: Redux.Dispatch<any>, onResultChange: () => void) => {
    const datasets = getDatasetList(reduxState, dispatch);
    return engine(_.map(datasets, d => ({
      key: d,
      name: toTitleCase(d),
      iconName: "database",
      href: `/explorer/${encodeURIComponent(d)}`,
    })), onResultChange);
  };
};

const standardProviders: { [id: string] : SearchProvider } = {
  assets: managedResourceProvider(
    selectors.selectAssets,
    assets,
    (id: string, item: Asset) => ({
      key: id,
      name: toTitleCase(item["Road Name"] || ""),
      description: item["RSL"],
      extra: toTitleCase(item["Section Description"] || ""),
      iconName: "drive-time", // A car :)
      href: `/assets/${encodeURIComponent(id)}`,
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
      iconName: "wrench",
      // TODO: what about the other RSLs?
      href: 
        item["RSLs"] && `/assets/${encodeURIComponent(item["RSLs"].split(",")[0])}`,
    }),
  ),

  // TODO: eliminate this
  people: staticProvider(["people"], _.map(["Arlo", "Alex", "Oliver"], p => ({
    key: p,
    name: p,
    iconName: "person",
    onSelect: () => toaster.show({ iconName: "person", intent: Intent.SUCCESS, message: `${p} clicked` }),
  }))),

  insights: staticProvider(["insights"], _.map(insights, (insight, name) => ({
    key: name,
    name: insight.title,
    iconName: "layout-auto",
    disabled: insight.disabled,
    href: `/insights/${encodeURIComponent(name)}`,
  }))),

  datasets: datasetProvider(),
};

export default standardProviders;
