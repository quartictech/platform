import * as React from "react";
import {
  Classes,
  Menu as BlueprintMenu,
  MenuDivider,
  MenuItem,
} from "@blueprintjs/core";
import { createStructuredSelector } from "reselect";
import { connect } from "react-redux";
import * as _ from "underscore";
import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  datasetList,
} from "../../api";
import {
  DatasetName,
} from "../../models";
import { appHistory } from "../../routes";
import { Header } from "../../components";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";


const s = require("./style.css");

interface AppProps {
  datasetListRequired: () => void;
  datasetList: ResourceState<DatasetName[]>;
}

interface MenuProps {
  datasetList: ResourceState<DatasetName[]>;
}

const Menu: React.SFC<MenuProps> = (props) => (
  <div className={s.menu}>
    <BlueprintMenu className={Classes.ELEVATION_3}>
      <MenuDivider title="Insights" />

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

      <MenuDivider title="Views" />

      <MenuItem iconName="database" text="Data explorer">
        {
          (props.datasetList.status !== ResourceStatus.LOADED)
            ? <MenuItem text="Dataset list unavailable" disabled={true} />
            : _.map(props.datasetList.data, d => (
                <MenuItem key={d} iconName="database" text={d} href={appHistory.createHref({
                  pathname: `/explorer/${encodeURIComponent(d)}`,
                })} />
              )
          )
        }
      </MenuItem>

      <MenuItem iconName="envelope" text="Messages" onClick={() => {}} />

    </BlueprintMenu>
  </div>
);

export class App extends React.Component<AppProps, void> {

  componentDidMount() {
    this.props.datasetListRequired();
  }

  render() {
    const { children } = this.props;
    return (
      <div>
      <section className={s.App}>
        <Header
          searchBoxChange={() => {}}
        />

      <div className={s.container}>
          <Menu
            datasetList={this.props.datasetList}
          />
          <div className={s.main}>
            {children}
          </div>
      </div>
      </section>
      </div>
    );
  }
}

const mapDispatchToProps = {
  datasetListRequired: resourceActions(datasetList).required,
  showNewDatasetModal: () => actions.setActiveModal("newDataset"),
};

const mapStateToProps = createStructuredSelector({
  datasetList: selectors.selectDatasetList,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
