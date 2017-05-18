import * as React from "react";
import { connect } from "react-redux";
import {
} from "@blueprintjs/core";
import { createStructuredSelector } from "reselect";
import * as _ from "underscore";
import PredictingPicker, { PredictingPickerEntry } from "../../components/PredictingPicker";

const rawEntries: PredictingPickerEntry[] = [
  {
    key: "123",
    name: "Arlo",
    description: "CEO",
    extra: "Arlo is a noob",
    category: "Humans",
  },
  {
    key: "456",
    name: "Alex",
    description: "CTO",
    extra: "Alex is a noob",
    category: "Humans",
  },
  {
    key: "789",
    name: "Oliver",
    description: "Head of Engineering",
    extra: "Oliver is a noob",
    category: "Humans",
  },
  {
    key: "ABC",
    name: "Edmund",
    description: "VP of Pugs",
    extra: "Edmund is a noob",
    category: "Animals",
  }
];

interface IProps {
}

interface IState {
  filteredEntries: PredictingPickerEntry[];
}

class SearchView extends React.Component<IProps, IState> {

  constructor(props) {
    super(props);
    this.state = {
      filteredEntries: rawEntries,
    };

    this.onNoobChange = this.onNoobChange.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  render() {
    return (
      <div>
        <p>Hello mummy</p>
        <PredictingPicker
              iconName="search"
              entryIconName="person"
              placeholder="Search noobs..."
              
              entries={this.state.filteredEntries}
              selectedKey={null}
              onChange={this.onNoobChange}
              errorDisabled={true}
              onQueryChange={this.onQueryChange}
        />
      </div>
    );
  }

  private onNoobChange(noob: string) {
    console.log("Selected:", noob);
  }

  private onQueryChange(query: string) {
    // TODO: shouldFilter is false when first interacted with, switches to true as soon as typing 
    // No filtering if disabled or if there's no text to filter by!
    if (/*!this.state.shouldFilter || */!query) {
      this.setState({ filteredEntries: rawEntries });
    }

    this.setState({ filteredEntries: _.chain(rawEntries)
      .filter(entry => stringInString(query, entry.name))
      .value()
    });
  }
}

function stringInString(needle: string, haystack: string) {
  return haystack.toLowerCase().includes(needle.toLowerCase());
}

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchView);
