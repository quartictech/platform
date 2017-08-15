import * as React from "react";
import { Link } from "react-router";
import {
  Classes,
  Intent,
  Menu,
  MenuItem,
  MenuDivider,
  Popover,
  Button,
  Position,
  Tooltip,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import { Profile } from "../../models";
const style = require("./style.css");
const logo = require("./quartic.svg");

interface IProps {
  newDatasetClick: any;
  searchBoxChange: any;
  selectedNamespace: string;
  namespaceSelectChange: any;
  onLogOutClick: () => void;
  namespaces: string[];
  profile?: Profile;
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
    const imgStyle = {
      height: "100%",
      paddingTop: "10px",
      paddingBottom: "10px",
      marginLeft: "-5px",
      marginRight: "10px",
    };

    return (
      <nav className={classNames(Classes.NAVBAR, Classes.FIXED_TOP, Classes.DARK)}>

        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_LEFT)}>
          <Link to="/" style={{ height: "100%", display: "inline-block" }}>
            <img
              style={imgStyle}
              src={logo}
              role="presentation"
            />
          </Link>
          <input
            className="pt-input"
            placeholder="Search datasets..."
            type="text"
            onChange={this.onSearch}
          />

          <span className={Classes.NAVBAR_DIVIDER} />

          <NamespaceList
            namespaces={this.props.namespaces}
            selectedNamespace={this.props.selectedNamespace}
            onChange={this.props.namespaceSelectChange}
          />

          <span className={Classes.NAVBAR_DIVIDER} />

          <Link
            className="pt-button pt-minimal pt-icon-database"
            to="/datasets"
          >
            Datasets
          </Link>
          <Link
            className="pt-button pt-minimal pt-icon-graph"
            to="/pipeline"
          >
            Pipeline
          </Link>

          <button
            onClick={this.props.newDatasetClick}
            className="pt-button pt-minimal pt-icon-cloud-upload"
          >
            Upload Data
          </button>
        </div>

        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_RIGHT)}>
          {this.maybeRenderProfile()}
        </div>
      </nav>
    );
  }

  private maybeRenderProfile() {
    if (!this.props.profile) {
      return null;
    }

    // A button is somewhat weird as it does nothing currently, but at least it renders in a nice way
    return (
        <Popover content={this.renderSettings()} position={Position.BOTTOM_RIGHT}>
            <Tooltip
              content="Settings"
              position={Position.BOTTOM_RIGHT}
              tooltipClassName={Classes.MINIMAL}
            >
              <div style={{ height: "100%", display: "block" }}>
                <Button
                  className={Classes.MINIMAL}
                  rightIconName="chevron-down"
                >
                  <img
                    className={style.profile}
                    src={this.props.profile.avatarUrl}
                  />
                  {this.props.profile.name}
                </Button>
              </div>
            </Tooltip>
        </Popover>
    );
  }

  private renderSettings() {
    return (
      <Menu>
        <MenuItem
          text={`Quartic version: ${process.env.BUILD_VERSION || "unknown"}`}
          iconName="info-sign"
          disabled={true}
        />
        <MenuDivider />
        <MenuItem
          text="Sign out"
          iconName="log-out"
          intent={Intent.DANGER}
          onClick={this.props.onLogOutClick}
        />
      </Menu>
    );
  }
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




export { Header };