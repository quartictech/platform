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

// TODO - eliminate assets/jobs as named props params

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

type AllProps = StateProps & DispatchProps & OwnProps;

interface State {
  cache: { [ id: string ] : PickerEntry[] };
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

class SearchContainer extends React.Component<AllProps, State> {
  constructor(props: AllProps) {
    super(props);
    this.state = { cache: {} };
    this.onEntrySelect = this.onEntrySelect.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  componentWillReceiveProps(nextProps: AllProps) {
    const cache = _.mapObject(this.providers(nextProps), (provider, name) =>
      provider.loaded ? provider.results : this.state.cache[name]);
    this.setState({ cache });
  }

  render() {
    return (
      <Picker
        className={this.props.className}
        iconName="search"
        defaultEntryIconName="person"
        placeholder={this.props.placeholder}
        entries={_.flatten(_.values(this.state.cache))}
        selectedKey={null}
        onEntrySelect={this.onEntrySelect}
        errorDisabled={true}
        onQueryChange={this.onQueryChange}
        working={_.any(this.providers(this.props), p => !p.loaded)}
      />
    );
  }

  // TODO: find a better way to construct routes
  private onEntrySelect(entry: PickerEntry) {
    (entry as any).onSelect();  // Slightly gross abuse
  }

  private onQueryChange(query: string) {
    _.forEach(this.providers(this.props), p => p.required(query));
  }

  private providers(props: AllProps): { [id: string] : SearchProvider } {
    return {
      "RSLs": managedResourceProvider(props.assetsClear, props.assetsRequired, props.assets, assetResults),
      "Jobs": managedResourceProvider(props.jobsClear, props.jobsRequired, props.jobs, jobResults),
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
