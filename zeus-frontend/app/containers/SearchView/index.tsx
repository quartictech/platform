import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import {
  Classes,
} from "@blueprintjs/core";
import * as classNames from "classnames";

import SearchContainer from "../../containers/SearchContainer";
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
      <DocumentTitle title="Quartic - Search">
        <div className={s.container} style={{ marginTop: "10%" }}>
          <SearchContainer
            className={classNames(Classes.LARGE, Classes.ROUND, s.myPicker)}
            placeholder="What do you want to know?"
          />
        </div>
      </DocumentTitle>
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
