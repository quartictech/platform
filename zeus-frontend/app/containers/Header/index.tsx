import * as React from "react";
import { connect } from "react-redux";
import { Link } from "react-router";
import { createStructuredSelector } from "reselect";
import {
  Button,
  Classes,
  Menu,
  MenuItem,
  Popover,
  Position,
  Tooltip,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";
import SearchContainer from "../../containers/SearchContainer";
import standardProviders from "../../containers/SearchContainer/standardProviders";
import insights from "../../containers/InsightView/insights";
import * as selectors from "../../redux/selectors";
import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  sessionInfo,
  datasetInfo,
} from "../../api";
import {
  DatasetInfo,
  SessionInfo,
} from "../../models";
import { appHistory } from "../../routes";
const styles = require("./style.css");
const logo = require("./quartic.svg");

interface Props {
  sessionInfoRequired: () => void;
  sessionInfo: ResourceState<SessionInfo>;
  datasetInfoRequired: () => void;
  datasetInfo: ResourceState<{ [id: string] : DatasetInfo}>;
}

class Header extends React.Component<Props, void> {
  componentDidMount() {
    this.props.sessionInfoRequired();
    this.props.datasetInfoRequired();
  }

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
          <SearchContainer
            className={classNames(Classes.ROUND, styles.myPicker)}
            placeholder="Search..."
            providers={standardProviders}
          />

          <span className={Classes.NAVBAR_DIVIDER} />

          <Popover content={this.renderInsightsMenu()} position={Position.BOTTOM} >
            <Button
              className={Classes.MINIMAL}
              iconName="layout-auto"
              rightIconName="chevron-down"
              text="Insights"
            />
          </Popover>

          <Popover content={this.renderExplorerMenu()} position={Position.BOTTOM} >
            <Button
              className={Classes.MINIMAL}
              iconName="database"
              rightIconName="chevron-down"
              text="Data explorer"
              disabled={this.props.datasetInfo.status !== ResourceStatus.LOADED}
            />
          </Popover>

        </div>

        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_RIGHT)}>

          {this.renderUser()}

          <span className={Classes.NAVBAR_DIVIDER} />

          <Popover content={this.renderSettings()} position={Position.BOTTOM_RIGHT}>
            <Tooltip content="Settings" position={Position.BOTTOM}>
              <Button className={Classes.MINIMAL} iconName="settings" />
            </Tooltip>
          </Popover>

        </div>
      </nav>);
  }

  private renderUser() {
    // A button is somewhat weird as it does nothing currently, but at least it renders in a nice way
    return (
      <Button
        className={Classes.MINIMAL}
        iconName="user"
        text={this.props.sessionInfo.data.username}
        loading={this.props.sessionInfo.status === ResourceStatus.LOADING}
      />
    );
  }

  private renderInsightsMenu() {
    return (
      <Menu>
        {_.map(insights, (insight, name) => (
          <MenuItem
            key={name}
            iconName="layout-auto"
            text={insight.title}
            disabled={insight.disabled}
            href={appHistory.createHref({ pathname: `/insights/${encodeURIComponent(name)}` })}
          />
        ))}
      </Menu>
    );
  }

  private renderExplorerMenu() {
    return (
      <Menu>
        {
          _.map(this.props.datasetInfo.data, (v, k) => (
            <MenuItem key={k} iconName="database" text={v.prettyName} href={appHistory.createHref({
              pathname: `/explorer/${encodeURIComponent(k)}`,
            })} />
          ))
        }
      </Menu>
    );
  }

  private renderSettings() {
    return (
      <Menu>
        <MenuItem
          key={"info"}
          text={`Quartic version: ${process.env.BUILD_VERSION || "unknown"}`}
          iconName="info-sign"
          disabled
        />
      </Menu>
    );
  }
}

const mapDispatchToProps = {
  sessionInfoRequired: resourceActions(sessionInfo).required,
  datasetInfoRequired: resourceActions(datasetInfo).required,
};

const mapStateToProps = createStructuredSelector({
  sessionInfo: selectors.selectSessionInfo,
  datasetInfo: selectors.selectDatasetInfo,
});

export default connect(mapStateToProps, mapDispatchToProps)(Header);
