import * as _ from "underscore";
import { Intent, IconClasses } from "@blueprintjs/core";
import { SearchProvider } from "./index";
import * as selectors from "../../redux/selectors";
import { toTitleCase } from "../../helpers/Utils";
import { toaster } from "../../containers/App/toaster";
import {
  resourceActions,
  ResourceStatus,
} from "../../api-management";
import {
  assets,
  jobs,
  datasetInfo,
} from "../../api";
import {
  Asset,
  Job,
} from "../../models";
import { managedResourceProvider } from "./managedResourceProvider";
import { staticProvider, staticProviderEngine } from "./staticProvider";
import insights from "../../containers/InsightView/insights";


const getDatasetInfo = (reduxState: any, dispatch: Redux.Dispatch<any>) => {
  const state = selectors.selectDatasetInfo(reduxState);
  if (state.status === ResourceStatus.NOT_LOADED) {
    dispatch(resourceActions(datasetInfo).required());
  }
  return state.data;
};

const datasetProvider = () => {
  const engine = staticProviderEngine(["datasets", "raw", "explorer"]);
  return (reduxState: any, dispatch: Redux.Dispatch<any>, onResultChange: () => void) => {
    const datasets = getDatasetInfo(reduxState, dispatch);
    return engine(
      _.map(datasets, (v, k) => ({
        key: k,
        name: v.prettyName,
        iconName: IconClasses.DATABASE,
        href: `/explorer/${encodeURIComponent(k)}`,
      })),
      onResultChange,
    );
  };
};

const standardProviders: { [id: string] : SearchProvider } = {
  assets: managedResourceProvider(
    selectors.selectAssets,
    assets,
    (id: string, item: Asset) => ({
      key: id,
      name: item["RSL"],
      description: toTitleCase(item["Road Name"] || ""),
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
    iconName: IconClasses.PERSON,
    onSelect: () => toaster.show({ iconName: "person", intent: Intent.SUCCESS, message: `${p} clicked` }),
  }))),

  insights: staticProvider(["insights"], _.map(insights, (insight, name) => ({
    key: name,
    name: insight.title,
    iconName: IconClasses.LAYOUT_AUTO,
    disabled: insight.disabled,
    href: `/insights/${encodeURIComponent(name)}`,
  }))),

  datasets: datasetProvider(),
};

export default standardProviders;
