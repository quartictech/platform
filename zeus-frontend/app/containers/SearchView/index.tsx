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
  noobs,
} from "../../api";
import {
  Noob,
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
    - limit results
    - order by relevance
    - handle backend errors nicely
    - controlled selection

  - Flow
    - click on result takes you to result page
    - press enter when none selected takes you to full results page

 *-----------------------------------------------------*/

interface IProps {
  noobsClear: () => void;
  noobsRequired: (string) => void;
  noobs: ResourceState<{ [id: string] : Noob }>;
}

interface IState {
  working: boolean;
}

class SearchView extends React.Component<IProps, IState> {
  constructor(props) {
    super(props);
    this.state = {
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
          placeholder="Search..."
          
          entries={this.results()}
          selectedKey={null}
          onChange={this.onNoobChange}
          errorDisabled={true}
          onQueryChange={this.onQueryChange}
          working={this.props.noobs.status === ResourceStatus.LOADING}
        />
      </div>
    );
  }

  // TODO: need to cache while working
  private results() {
    return _.map(this.props.noobs.data,
      (noob, id: string) => ({
        key: id,
        name: noob.name,
        description: noob.role,
        category: noob.category,
        extra: `${noob.name} is a noob`,
      } as PredictingPickerEntry)
    );
  }

  private onNoobChange(noob: string) {
    console.log("Selected:", noob);
  }

  private onQueryChange(query: string) {
    if (query.length > 0) {
      this.props.noobsRequired(query);
    } else {
      this.props.noobsClear();
    }
  }
}

const mapDispatchToProps = {
  noobsClear: resourceActions(noobs).clear,
  noobsRequired: resourceActions(noobs).required,
};

const mapStateToProps = createStructuredSelector({
  noobs: selectors.selectNoobs,
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchView);
