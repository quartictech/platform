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
} from "../../api";
import {
  Asset,
} from "../../models";

const logo = require("./quartic.svg");

interface HeaderProps {
  entriesClear: () => void;
  entriesRequired: (string, int) => void;
  entries: ResourceState<{ [id: string] : Asset }>;
}

class Header extends React.Component<HeaderProps, void> {
  render() {
    return (
      <nav className="pt-navbar .modifier pt-dark">
        <div className="pt-navbar-group pt-align-left">
          <Link to="/" style={{ height: "100%", display: "inline-block" }}>
            <img
              style={{
                height: "100%",
                paddingTop: "10px",
                paddingBottom: "10px",
                marginLeft: "-5px",
                marginRight: "10px"
              }}
              src={logo}
              role="presentation"
            >
            </img>
          </Link>
          <Search
            className={classNames(Classes.DARK, Classes.ROUND)}
            entriesClear={this.props.entriesClear}
            entriesRequired={this.props.entriesRequired}
            entries={this.props.entries}
            placeholder="Search..."
          />
        </div>
        <div className="pt-navbar-group pt-align-right">
          <span className="pt-navbar-divider"></span>
          <a href="/map" alt="Map" className="pt-button pt-minimal pt-icon-map" />
        </div>
      </nav>);
  }
}

const mapDispatchToProps = {
  entriesClear: resourceActions(assets).clear,
  entriesRequired: resourceActions(assets).required,
};

const mapStateToProps = createStructuredSelector({
  entries: selectors.selectAssets,
});

export default connect(mapStateToProps, mapDispatchToProps)(Header);

