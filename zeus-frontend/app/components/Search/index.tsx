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
  entriesClear: () => void;
  entriesRequired: (string, int) => void;
  entries: ResourceState<{ [id: string] : Asset }>;
  className?: string;
}

interface SearchState {
  entries: { [id: string] : Asset };
}

export default class Search extends React.Component<SearchProps, SearchState> {
  constructor(props) {
    super(props);
    this.state = {
      entries: {}
    };

    this.onEntrySelect = this.onEntrySelect.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  public componentWillReceiveProps(nextProps: SearchProps) {
    // Cache current results whilst working
    if (nextProps.entries.status !== ResourceStatus.LOADING) {
      this.setState({ entries: nextProps.entries.data });
    }
  }

  render() {
    return (
      <Picker
        className={this.props.className}
        iconName="search"
        defaultEntryIconName="person"
        placeholder="What do you want to know?"
        
        entries={this.results()}
        selectedKey={null}
        onEntrySelect={this.onEntrySelect}
        errorDisabled={true}
        onQueryChange={this.onQueryChange}
        working={this.props.entries.status === ResourceStatus.LOADING}
      />
    );
  }

  private results() {
    return _.map(this.state.entries,
      (entry, id: string) => ({
        key: id,
        name: toTitleCase(entry["Road Name"] || ""),
        description: entry["RSL"],
        extra: toTitleCase(entry["Section Description"] || ""),
        category: "RSLs",
        iconName: "drive-time", // A car :)
      } as PickerEntry)
    );
  }

  // TODO: find a better way to construct routes
  private onEntrySelect(key: string) {
    appHistory.push(`/assets/${encodeURIComponent(key)}`);
  }

  private onQueryChange(query: string) {
    if (query.length > 0) {
      this.props.entriesRequired(query, 5); // Limit results to keep things responsive
    } else {
      this.props.entriesClear();
    }
  }
}
