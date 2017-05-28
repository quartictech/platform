import * as React from "react";
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";

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

const SearchContainer: React.SFC<StateProps & DispatchProps & OwnProps> = (props) => (
  <Search
    className={props.className}
    assetsClear={props.assetsClear}
    assetsRequired={props.assetsRequired}
    assets={props.assets}
    jobsClear={props.jobsClear}
    jobsRequired={props.jobsRequired}
    jobs={props.jobs}
    placeholder={props.placeholder}
  />
);

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(SearchContainer);
