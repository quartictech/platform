import * as React from "react";
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import {
  Classes,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import Search from "../../components/Search";
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

interface SearchViewProps {
  entriesClear: () => void;
  entriesRequired: (string, int) => void;
  entries: ResourceState<{ [id: string] : Asset }>;
}

class SearchView extends React.Component<SearchViewProps, {}> {
  public componentWillReceiveProps(nextProps: SearchViewProps) {
    // Cache current results whilst working
    if (nextProps.entries.status !== ResourceStatus.LOADING) {
      this.setState({ entries: nextProps.entries.data });
    }
  }

  render() {
    return (
      <div className={s.container} style={{ marginTop: "25%" }}>
        <Search
          className={classNames(Classes.LARGE, Classes.ROUND, s.myPicker)}
          entriesClear={this.props.entriesClear}
          entriesRequired={this.props.entriesRequired}
          entries={this.props.entries}
          placeholder="What do you want to know?"
        />
      </div>
    );
  }
}

const mapDispatchToProps = {
  entriesClear: resourceActions(assets).clear,
  entriesRequired: resourceActions(assets).required,
};

const mapStateToProps = createStructuredSelector({
  entries: selectors.selectAssets,
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchView);
