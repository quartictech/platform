import * as React from "react";
import * as Redux from "redux";
import { connect } from "react-redux";
import * as _ from "underscore";

import Picker, { PickerEntry } from "../../components/Picker";
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
  states: { [id: string] : ResourceState<any> };
}

interface DispatchProps {
  dispatch: Redux.Dispatch<any>;
}

interface OwnProps {
  className?: string;
  placeholder?: string;
}

type AllProps = StateProps & DispatchProps & OwnProps;

interface State {
  cache: { [ id: string ] : PickerEntry[] };
}

// TODO - get rid of props currying
interface SearchProvider<T> {
  selector: (state) => ResourceState<{ [id: string] : T }>;
  required: (props: AllProps) => (string) => void;
  results: (props: AllProps) => PickerEntry[],
  loaded: (props: AllProps) => boolean;
}

class SearchContainer extends React.Component<AllProps, State> {
  constructor(props: AllProps) {
    super(props);
    console.log(props);
    this.state = { cache: {} };
  }

  componentWillReceiveProps(nextProps: AllProps) {
    const cache = _.mapObject(providers, (provider, name) =>
      provider.loaded(nextProps) ? provider.results(nextProps) : this.state.cache[name]);
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
        onEntrySelect={entry => (entry as any).onSelect()}
        errorDisabled={true}
        onQueryChange={query => _.forEach(providers, p => p.required(this.props)(query))}
        working={_.any(providers, p => !p.loaded(this.props))}
      />
    );
  }
}

function managedResourceProvider<T>(
  resource: ManagedResource<{ [id: string] : T }>,
  state: (props: AllProps) => ResourceState<{ [id: string] : T }>,
  mapper: (id: string, item: T) => PickerEntry,
  selector
) {
  return {
    required: (props: AllProps) => (query: string) => {
      if (query.length > 0) {
        props.dispatch(resourceActions(resource).required(query, 5));
      } else {
        props.dispatch(resourceActions(resource).clear());
      }
    },
    results: (props) => _.map(state(props).data, (item, id) => mapper(id, item)),
    loaded: (props) => state(props).status !== ResourceStatus.LOADING,
    selector,
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

const providers: { [id: string] : SearchProvider<any> } = {
  assets: managedResourceProvider(
    assets,
    null,
    assetResults,
    selectors.selectAssets
  ),
  jobs: managedResourceProvider(
    jobs,
    null,
    jobResults,
    selectors.selectJobs
  ),
};

const mapStateToProps = (state) => ({
  states: _.mapObject(providers, p => p.selector(state)),
});

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, null)(SearchContainer);
