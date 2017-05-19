import * as React from "react";
import { connect } from "react-redux";
import {
  Classes,
} from "@blueprintjs/core";
import { createStructuredSelector } from "reselect";
import * as _ from "underscore";
import * as classNames from "classnames";
import PredictingPicker, { PredictingPickerEntry } from "../../components/PredictingPicker";
import * as selectors from "../../redux/selectors";
import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  assets,
} from "../../api";
import {
  Asset,
} from "../../models";

const s = require("./style.css");

/*-----------------------------------------------------*
  TODO

  - Appearance
    - vertical center
    - sensible width (both input control + menu)
    - fix inverse colouring
    - stop the jerking around due to spinner appearing/disappearing
    - boldness
    - scrolling (see https://github.com/palantir/blueprint/pull/1049)

  - Behaviour
    - debouncing
    - order by relevance
    - handle backend errors nicely
    - controlled selection

  - Flow
    - click on result takes you to result page
    - press enter when none selected takes you to full results page

 *-----------------------------------------------------*/

interface SearchViewProps {
  entriesClear: () => void;
  entriesRequired: (string, int) => void;
  entries: ResourceState<{ [id: string] : Asset }>;
}

interface SearchViewState {
  entries: { [id: string] : Asset };
}

class SearchView extends React.Component<SearchViewProps, SearchViewState> {
  constructor(props) {
    super(props);
    this.state = {
      entries: {}
    };

    this.onNoobChange = this.onNoobChange.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  public componentWillReceiveProps(nextProps: SearchViewProps) {
    // Cache current results whilst working
    if (nextProps.entries.status !== ResourceStatus.LOADING) {
      this.setState({ entries: nextProps.entries.data });
    }
  }

  render() {
    return (
      <div className={s.container}>
        <PredictingPicker
          className={classNames(Classes.LARGE, Classes.ROUND)}
          iconName="search"
          defaultEntryIconName="person"
          placeholder="Search..."
          
          entries={this.results()}
          selectedKey={null}
          onChange={this.onNoobChange}
          errorDisabled={true}
          onQueryChange={this.onQueryChange}
          working={this.props.entries.status === ResourceStatus.LOADING}
        />
      </div>
    );
  }

  // TODO: need to cache while working
  private results() {
    return _.map(this.state.entries,
      (entry, id: string) => ({
        key: id,
        name: toTitleCase(entry["Road Name"] || ""),
        description: entry["RSL"],
        extra: toTitleCase(entry["Section Description"] || ""),
        category: "RSLs",
        iconName: "drive-time", // A car :)
      } as PredictingPickerEntry)
    );
  }

  private onNoobChange(noob: string) {
    console.log("Selected:", noob);
  }

  private onQueryChange(query: string) {
    if (query.length > 0) {
      this.props.entriesRequired(query, 5); // Limit results to keep things responsive
    } else {
      this.props.entriesClear();
    }
  }
}

// From http://stackoverflow.com/a/196991
const toTitleCase = (str) =>
  str.replace(/\w\S*/g, txt => txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase());

const mapDispatchToProps = {
  entriesClear: resourceActions(assets).clear,
  entriesRequired: resourceActions(assets).required,
};

const mapStateToProps = createStructuredSelector({
  entries: selectors.selectAssets,
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchView);
