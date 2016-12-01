import * as React from "react";
import { Link } from "react-router";

const s = require("./style.css");
const logo = require("./quartic.svg");

interface IProps {
  newDatasetClick: any;
  searchBoxChange: any;
}

class Header extends React.Component<IProps, void> {
  onSearch(e) {
    this.props.searchBoxChange(e.target.value);
  }

  render() {
    return (
      <nav className="pt-navbar .modifier pt-dark">
        <div className="pt-navbar-group pt-align-left">
          <Link to="/" className={s.logo}>
            <img
              className={s.logo}
              src={logo}
              role="presentation"
              data-content={`Version: ${(process.env.BUILD_VERSION
                || "unknown")}`}
              data-variation="mini"
            >
            </img>
          </Link>
          <input
            className="pt-input"
            placeholder="Search datasets..."
            type="text"
            onChange={this.onSearch.bind(this)}
          />
          <span className="pt-navbar-divider"></span>
          <button
            onClick={this.props.newDatasetClick}
            className="pt-button pt-minimal pt-icon-upload"
          >
            Upload Data
          </button>
        </div>
        <div className="pt-navbar-group pt-align-right">
          <span className="pt-navbar-divider"></span>
          <a href="/map" alt="Map" className="pt-button pt-minimal pt-icon-map" />
        </div>
      </nav>);
  }
}

export { Header }
