// TODO - this is really a container
import * as React from "react";
import { connect } from "react-redux";
import { Link } from "react-router";
import { createStructuredSelector } from "reselect";
import {
  AnchorButton,
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
import { toTitleCase } from "../../helpers/Utils";
import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  datasetList,
} from "../../api";
import {
  Asset,
  Job,
  DatasetName,
} from "../../models";
import { appHistory } from "../../routes";
const styles = require("./style.css");
const logo = require("./quartic.svg");

interface HeaderProps {
  assetsClear: () => void;
  assetsRequired: (string, int) => void;
  assets: ResourceState<{ [id: string] : Asset }>;

  jobsClear: () => void;
  jobsRequired: (string, int) => void;
  jobs: ResourceState<{ [id: string] : Job }>;

  datasetListRequired: () => void;
  datasetList: ResourceState<DatasetName[]>;
}

class Header extends React.Component<HeaderProps, void> {
  componentDidMount() {
    this.props.datasetListRequired();
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
              text="Raw data explorer"
              disabled={this.props.datasetList.status !== ResourceStatus.LOADED}
            />
          </Popover>

        </div>

        <div className={classNames(Classes.NAVBAR_GROUP, Classes.ALIGN_RIGHT)}>
          <span className={Classes.NAVBAR_DIVIDER} />

          <Popover content={this.renderSettings()} position={Position.BOTTOM}>
            <Tooltip content="Settings" position={Position.BOTTOM}>
              <Button className={Classes.MINIMAL} iconName="settings" />
            </Tooltip>
          </Popover>

          <Tooltip content="Map" position={Position.BOTTOM}>
            <AnchorButton disabled={true} className={Classes.MINIMAL} iconName="globe" href="/map" />
          </Tooltip>
        </div>
      </nav>);
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
          _.map(this.props.datasetList.data, d => (
            <MenuItem key={d} iconName="database" text={toTitleCase(d)} href={appHistory.createHref({
              pathname: `/explorer/${encodeURIComponent(d)}`,
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
          text={`Quartic Inbox (version: ${process.env.BUILD_VERSION || "unknown"})`}
          iconName="info-sign"
          disabled
        />
      </Menu>
    );
  }
}

const mapDispatchToProps = {
  datasetListRequired: resourceActions(datasetList).required,
};

const mapStateToProps = createStructuredSelector({
  datasetList: selectors.selectDatasetList,
});

export default connect(mapStateToProps, mapDispatchToProps)(Header);
