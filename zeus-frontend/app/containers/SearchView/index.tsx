import * as React from "react";
import { connect } from "react-redux";
import {
  Classes,
} from "@blueprintjs/core";
import { createStructuredSelector } from "reselect";
import * as _ from "underscore";
import * as classNames from "classnames";
import PredictingPicker, { PredictingPickerEntry } from "../../components/PredictingPicker";
const s = require("./style.css");

// TODO - layout page
// TODO - sort out inverse colouring
// TODO - hook up to backend via Redux + API
// TODO - ensure selection callback is working properly
// TODO - handle API errors nicely
// TODO - (backend) substring search
// TODO - order by relevance rather than alphabetic?
// TODO - scrolling


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
  },
  {
    key: "DEF",
    name: "Puss",
    description: "VP of Cats",
    extra: "Puss is a noob",
    category: "Animals",
  },
];

interface IProps {
}

interface IState {
  filteredEntries: PredictingPickerEntry[];
  working: boolean;
}

class SearchView extends React.Component<IProps, IState> {

  constructor(props) {
    super(props);
    this.state = {
      filteredEntries: rawEntries,
      working: true,
    };

    this.onNoobChange = this.onNoobChange.bind(this);
    this.onQueryChange = this.onQueryChange.bind(this);
  }

  render() {
    return (
      <div className={s.container}>
        <PredictingPicker
          className={classNames(Classes.LARGE, Classes.ROUND)}
          iconName="search"
          entryIconName="person"
          placeholder="What do you want to know?"
          
          entries={this.state.filteredEntries}
          selectedKey={null}
          onChange={this.onNoobChange}
          errorDisabled={true}
          onQueryChange={this.onQueryChange}
          working={this.state.working}
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
