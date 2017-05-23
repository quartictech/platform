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
import Search from "../../components/Search";
import * as selectors from "../../redux/selectors";
import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  assets,
  jobs,
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
      <nav className={classNames(Classes.NAVBAR)}>

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
            className={classNames(Classes.ROUND, styles.myPicker)}
            assetsClear={this.props.assetsClear}
            assetsRequired={this.props.assetsRequired}
            assets={this.props.assets}
            jobsClear={this.props.jobsClear}
            jobsRequired={this.props.jobsRequired}
            jobs={this.props.jobs}
            placeholder="Search..."
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
            <AnchorButton className={Classes.MINIMAL} iconName="globe" href="/map" />
          </Tooltip>
        </div>
      </nav>);
  }

  private renderInsightsMenu() {
    return (
      <Menu>
        <MenuItem iconName="layout-auto" text="All" href={appHistory.createHref({
          pathname: "/insights",
        })} />
        <MenuItem iconName="layout-auto" text="Failure predictions" href={appHistory.createHref({
          pathname: "/insights",
          query: { insightType: "failure" },
        })} />
        <MenuItem iconName="layout-auto" text="Incident clusters" href={appHistory.createHref({
          pathname: "/insights",
          query: { insightType: "cluster" },
        })} />
        <MenuItem iconName="layout-auto" text="SmartOps" href={appHistory.createHref({
          pathname: "/insights",
          query: { insightType: "smartops" },
        })} />
      </Menu>
    );
  }

  private renderExplorerMenu() {
    return (
      <Menu>
        {
          _.map(this.props.datasetList.data, d => (
            <MenuItem key={d} iconName="database" text={d} href={appHistory.createHref({
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
  assetsClear: resourceActions(assets).clear,
  assetsRequired: resourceActions(assets).required,
  jobsClear: resourceActions(jobs).clear,
  jobsRequired: resourceActions(jobs).required,
  datasetListRequired: resourceActions(datasetList).required,
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
  jobs: selectors.selectJobs,
  datasetList: selectors.selectDatasetList,
});

export default connect(mapStateToProps, mapDispatchToProps)(Header);

