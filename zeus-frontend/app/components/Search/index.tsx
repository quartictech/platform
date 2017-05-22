import * as React from "react";
import * as _ from "underscore";
import Picker, { PickerEntry } from "../../components/Picker";
import { appHistory } from "../../routes";
import { toTitleCase } from "../../helpers/Utils";
import {
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  Asset,
  Job,
} from "../../models";

/*-----------------------------------------------------*
  TODO

  - Appearance
    - vertical center
    - fix inverse colouring
    - boldness
    - scrolling (see https://github.com/palantir/blueprint/pull/1049)

  - Behaviour
    - debouncing
    - order by relevance
    - handle backend errors nicely
    - controlled selection

 *-----------------------------------------------------*/

interface SearchProps {
  assetsClear: () => void;
  assetsRequired: (string, int) => void;
  assets: ResourceState<{ [id: string] : Asset }>;

  jobsClear: () => void;
  jobsRequired: (string, int) => void;
  jobs: ResourceState<{ [id: string] : Job }>;
  
  placeholder?: string;
  className?: string;
}

interface SearchState {
  cachedAssets: { [id: string] : Asset };
  cachedJobs: { [id: string] : Job };
}

export default class Search extends React.Component<SearchProps, SearchState> {
  constructor(props) {
    super(props);
    this.state = {
      cachedAssets: {},
      cachedJobs: {},
    };

    this.onEntrySelect = this.onEntrySelect.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  public componentWillReceiveProps(nextProps: SearchProps) {
    // Cache current results whilst working
    if (nextProps.assets.status !== ResourceStatus.LOADING) {
      this.setState({ cachedAssets: nextProps.assets.data });
    }
    if (nextProps.jobs.status !== ResourceStatus.LOADING) {
      this.setState({ cachedJobs: nextProps.jobs.data });
    }
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
        working={
          (this.props.assets.status === ResourceStatus.LOADING) ||
          (this.props.jobs.status === ResourceStatus.LOADING)
        }
      />
    );
  }
  private results() {
    return _.flatten([this.assetResults(), this.jobResults()]);
  }
  
  private assetResults() {
    return _.map(this.state.cachedAssets,
      (entry, id: string) => ({
        key: id,
        name: toTitleCase(entry["Road Name"] || ""),
        description: entry["RSL"],
        extra: toTitleCase(entry["Section Description"] || ""),
        category: "RSLs",
        iconName: "drive-time", // A car :)
        onSelect: () => appHistory.push(`/assets/${encodeURIComponent(id)}`),
      } as PickerEntry)
    );
  }

  private jobResults() {
    return _.map(this.state.cachedJobs,
      (entry, id: string) => ({
        key: id,
        name: toTitleCase(entry["Number"] || ""),
        description: entry["RSLs"],
        extra: entry["Type"],
        category: "Jobs",
        iconName: "wrench",
        onSelect: () => appHistory.push(`/assets/${encodeURIComponent(entry["RSLs"].split(",")[0])}`),  // TODO: what about the other RSLs?
      } as PickerEntry)
    );
  }

  // TODO: find a better way to construct routes
  private onEntrySelect(entry: PickerEntry) {
    (entry as any).onSelect();  // Slightly gross abuse
  }

  private onQueryChange(query: string) {
    if (query.length > 0) {
      this.props.assetsRequired(query, 5); // Limit results to keep things responsive
      this.props.jobsRequired(query, 5);
    } else {
      this.props.assetsClear();
      this.props.jobsClear();
    }
  }
}
