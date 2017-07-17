import * as React from "react";
import { Link } from "react-router";

import { Menu, MenuItem, MenuDivider, Popover, Button, Position } from "@blueprintjs/core";

const s = require("./style.css");
const logo = require("./quartic.svg");

interface IProps {
  newDatasetClick: any;
  searchBoxChange: any;
  selectedNamespace: string;
  namespaceSelectChange: any;
  namespaces: string[];
}


interface INamespaceListProps {
  namespaces: string[];
  selectedNamespace: string;
  onChange: any;
}

class NamespaceList extends React.Component<INamespaceListProps, {}> {
  constructor() {
    super();
  }

  renderNamespaceMenu() {
    return this.props.namespaces.map(ns => <MenuItem key={ns} onClick={() => this.props.onChange(ns)} text={ns} />);
  }

  public render() {
    const content = (
      <Menu>
        <MenuItem text="All" onClick={() => this.props.onChange(null)} />
        <MenuDivider />
        {this.renderNamespaceMenu()}
      </Menu>
    );
    return (
      <Popover
        content={content}
        position={Position.BOTTOM}
      >
        <Button text={this.props.selectedNamespace || "All"} />
      </Popover>
    );
  }
}

class Header extends React.Component<IProps, void> {
  constructor(props: IProps) {
    super(props);
    this.onSearch = this.onSearch.bind(this);
  }

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
              data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
              data-variation="mini"
            />
          </Link>
          <input
            className="pt-input"
            placeholder="Search datasets..."
            type="text"
            onChange={this.onSearch}
          />
          <span className="pt-navbar-divider" />

          <NamespaceList
            namespaces={this.props.namespaces}
            selectedNamespace={this.props.selectedNamespace}
            onChange={this.props.namespaceSelectChange}
          />
          <span className="pt-navbar-divider" />

          <button
            onClick={this.props.newDatasetClick}
            className="pt-button pt-minimal pt-icon-cloud-upload"
          >
            Upload Data
          </button>
        </div>
        <div className="pt-navbar-group pt-align-right">
          <span className="pt-navbar-divider" />
          <a href="/map" alt="Map" className="pt-button pt-minimal pt-icon-map" />
        </div>
      </nav>);
  }
}

export { Header };
