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
  onLogOutClick: () => void;
  profile?: Profile;
}

class Header extends React.Component<IProps, {}> {
  constructor(props: IProps) {
    super(props);
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


        </div>

        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_RIGHT)}>
          <a
            className="pt-button pt-minimal pt-icon-help"
            href="https://docs.quartic.io"
            target="_blank"
          />
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
                  className={classNames(Classes.MINIMAL)}
                  style={{ lineHeight: "50px" }}
                  rightIconName="chevron-down"
                  text={this.props.profile.name}
                >
                  <img
                    className={style.profile}
                    src={this.props.profile.avatarUrl}
                  />
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

export { Header };
