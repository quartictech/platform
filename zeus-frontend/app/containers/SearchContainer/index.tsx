import * as React from "react";
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as _ from "underscore";

import Picker, { PickerEntry } from "../../components/Picker";
import * as selectors from "../../redux/selectors";
import { appHistory } from "../../routes";
import { toTitleCase } from "../../helpers/Utils";
import {
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

/*-----------------------------------------------------*
  TODO

  - Appearance
    - boldness
    - scrolling (see https://github.com/palantir/blueprint/pull/1049)

  - Behaviour
    - debouncing
    - order by relevance
    - handle backend errors nicely
    - controlled selection

 *-----------------------------------------------------*/

interface StateProps {
  assets: ResourceState<{ [id: string] : Asset }>;
  jobs: ResourceState<{ [id: string] : Job }>;
}

interface DispatchProps {
  assetsClear: () => void;
  assetsRequired: (string, int) => void;
  jobsClear: () => void;
  jobsRequired: (string, int) => void;  
}

interface OwnProps {
  className?: string;
  placeholder?: string;
}

interface SearchProvider {
  required: (string) => void;
  results: PickerEntry[],
  loaded: boolean;
}

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
  jobs: selectors.selectJobs,
});

const mapDispatchToProps = {
  assetsClear: resourceActions(assets).clear,
  assetsRequired: resourceActions(assets).required,
  jobsClear: resourceActions(jobs).clear,
  jobsRequired: resourceActions(jobs).required,
};

class SearchContainer extends React.Component<StateProps & DispatchProps & OwnProps, {}> {
  constructor(props: StateProps & DispatchProps & OwnProps) {
    super(props);
    this.onEntrySelect = this.onEntrySelect.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  render() {
    return (
      <Picker
        className={this.props.className}
        iconName="search"
        defaultEntryIconName="person"
        placeholder={this.props.placeholder}
        entries={this.results()}
        selectedKey={null}
        onEntrySelect={this.onEntrySelect}
        errorDisabled={true}
        onQueryChange={this.onQueryChange}
        working={_.any(this.providers(), p => !p.loaded)}
      />
    );
  }

  // TODO: find a better way to construct routes
  private onEntrySelect(entry: PickerEntry) {
    (entry as any).onSelect();  // Slightly gross abuse
  }

  private onQueryChange(query: string) {
    _.forEach(this.providers(), p => p.required(query));
  }

  private results() {
    return _.flatten(_.map(this.providers(), p => p.results));
  }

  private providers(): { [id: string] : SearchProvider } {
    return {
      "RSLs": managedResourceProvider(this.props.assetsClear, this.props.assetsRequired, this.props.assets, assetResults),
      "Jobs": managedResourceProvider(this.props.jobsClear, this.props.jobsRequired, this.props.jobs, jobResults),
    };
  }
}

function managedResourceProvider<T>(
  clear: () => void,
  required: (string, int) => void,
  state: ResourceState<{ [id: string] : T }>,
  mapper: (id: string, item: T) => PickerEntry
) {
  return {
    required: (query: string) => {
      if (query.length > 0) {
        required(query, 5);
      } else {
        clear();
      }
    },
    results: _.map(state.data, (item, id) => mapper(id, item)),
    loaded: state.status !== ResourceStatus.LOADING,
  };
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

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(SearchContainer);
