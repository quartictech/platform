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
} from "../../api-management";
import {
  assets,
  jobs,
} from "../../api";
import {
  Asset,
  Job,
} from "../../models";

const s = require("./style.css");

interface SearchViewProps {
  assetsClear: () => void;
  assetsRequired: (string, int) => void;
  assets: ResourceState<{ [id: string] : Asset }>;

  jobsClear: () => void;
  jobsRequired: (string, int) => void;
  jobs: ResourceState<{ [id: string] : Job }>;
}

class SearchView extends React.Component<SearchViewProps, {}> {
  render() {
    return (
      <div className={s.container} style={{ marginTop: "10%" }}>
        <Search
          className={classNames(Classes.LARGE, Classes.ROUND, s.myPicker)}
          assetsClear={this.props.assetsClear}
          assetsRequired={this.props.assetsRequired}
          assets={this.props.assets}
          jobsClear={this.props.jobsClear}
          jobsRequired={this.props.jobsRequired}
          jobs={this.props.jobs}
          placeholder="What do you want to know?"
        />
      </div>
    );
  }
}

const mapDispatchToProps = {
  assetsClear: resourceActions(assets).clear,
  assetsRequired: resourceActions(assets).required,
  jobsClear: resourceActions(jobs).clear,
  jobsRequired: resourceActions(jobs).required,
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
  jobs: selectors.selectJobs,
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchView);
