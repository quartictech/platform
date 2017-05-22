import * as React from "react";
import { connect } from "react-redux";
import { Link } from "react-router";
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

const logo = require("./quartic.svg");

interface HeaderProps {
  assetsClear: () => void;
  assetsRequired: (string, int) => void;
  assets: ResourceState<{ [id: string] : Asset }>;

  jobsClear: () => void;
  jobsRequired: (string, int) => void;
  jobs: ResourceState<{ [id: string] : Job }>;
}

class Header extends React.Component<HeaderProps, void> {
  render() {
    return (
      <nav className={classNames(Classes.NAVBAR, Classes.DARK)}>
        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_LEFT)}>
          <Link to="/" style={{ height: "100%", display: "inline-block" }}>
            <img
              style={{
                height: "100%",
                paddingTop: "10px",
                paddingBottom: "10px",
                marginLeft: "-5px",
                marginRight: "10px",
              }}
              src={logo}
              role="presentation"
            >
            </img>
          </Link>
          <Search
            className={classNames(Classes.DARK, Classes.ROUND)}
            assetsClear={this.props.assetsClear}
            assetsRequired={this.props.assetsRequired}
            assets={this.props.assets}
            jobsClear={this.props.jobsClear}
            jobsRequired={this.props.jobsRequired}
            jobs={this.props.jobs}
            placeholder="Search..."
          />
        </div>
        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_RIGHT)}>
          <span className={Classes.NAVBAR_DIVIDER} />
          <a href="/map" alt="Map" className={classNames(Classes.BUTTON, Classes.MINIMAL, Classes.iconClass("map"))} />
        </div>
      </nav>);
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

export default connect(mapStateToProps, mapDispatchToProps)(Header);

