import * as React from "react";
import { Header } from "../../components";

import { createStructuredSelector } from "reselect";
import * as actions from "../../redux/actions";
import { connect } from "react-redux";
import { Link } from "react-router";

const s = require("./style.css");

interface IProps {
  children?: any;
  location?: {
    pathname: string
  };
  params?: {
    node: string
  };
}

const Menu = () => (
 <div className={s.menu}>
          <ul className="pt-menu pt-elevation-1">
  <li className="pt-menu-header"><h6>Insights</h6></li>
  <li><Link className="pt-menu-item pt-icon-layout-auto" to={{ pathname: "/insights" }}>All</Link></li>
  <li><Link className="pt-menu-item pt-icon-layout-auto" to={{ pathname: "/insights", query: {insightType: "failure"}}}>Failure Predictions</Link></li>
  <li><Link className="pt-menu-item pt-icon-layout-auto" to={{ pathname: "/insights", query: {insightType: "cluster"}}}>Incident Clusters</Link></li>
  <li><Link className="pt-menu-item pt-icon-layout-auto" to={{ pathname: "/insights", query: {insightType: "smartops"}}}>SmartOps</Link></li>
  <li className="pt-menu-header"><h6>Views</h6></li>
  <li><Link className="pt-menu-item pt-icon-history" to={`/inventory`}>Inventory</Link></li>
  <li><button type="button" className="pt-menu-item pt-icon-envelope">Messages</button></li>
</ul>
</div>
);

export class App extends React.Component<IProps, void> {
  render() {
    const { children } = this.props;
    return (
      <div>
      <section className={s.App}>
        <Header
          searchBoxChange={() => {}}
        />

      <div className={s.container}>
          <Menu />
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
  showNewDatasetModal: () => actions.setActiveModal("newDataset"),
};

const mapStateToProps = createStructuredSelector({
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
