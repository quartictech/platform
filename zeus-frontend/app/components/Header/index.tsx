import * as React from "react";
import { Link } from "react-router";

const logo = require("./quartic.svg");

interface IProps {
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
          <input
            className="pt-input"
            placeholder="Search knowledge base..."
            type="text"
            onChange={this.onSearch.bind(this)}
          />
        </div>
        <div className="pt-navbar-group pt-align-right">
          <span className="pt-navbar-divider"></span>
          <a href="/map" alt="Map" className="pt-button pt-minimal pt-icon-map" />
        </div>
      </nav>);
  }
}

export { Header }
