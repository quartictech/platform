import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { IInsight } from "../../models";
import * as classNames from "classnames";
const s = require("./style.css");

import { Link } from "react-router";

interface IProps {
  ui: any;
  insights:IInsight[];
  closeNewDatasetModal: any;
}

interface IState {
  datasetId: string;
};

const Insight = ({ insight }) => (
  <div className={classNames(s.insight, "pt-card", "pt-elevation-2", "pt-interactive")}>
    <div className="pt-callout pt-icon-warning-sign" style={{backgroundColor: "#ffffff"}}>
     <h5>{ insight.title }</h5>
         <div dangerouslySetInnerHTML={{__html: insight.body}}>
           </div>
           </div>
    <div>
      <Link className="pt-button pt-intent-primary" to={`/insights/${insight.id}`}>
      See detail
      </Link>
    </div>
  </div>
);

const Menu = () => (
 <div className={s.menu}>
          <ul className="pt-menu pt-elevation-1">
  <li className="pt-menu-header"><h6>Insights</h6></li>
  <li><button type="button" className="pt-menu-item pt-icon-layout-auto">Failure Predictions</button></li>
  <li><button type="button" className="pt-menu-item pt-icon-layout-auto">Incident Clustering</button></li>
  <li><button type="button" className="pt-menu-item pt-icon-layout-circle">Unusual Conditions</button></li>
  <li className="pt-menu-header"><h6>Views</h6></li>
  <li><button type="button" className="pt-menu-item pt-icon-history">History</button></li>
  <li><button type="button" className="pt-menu-item pt-icon-star">Favorites</button></li>
  <li><button type="button" className="pt-menu-item pt-icon-envelope">Messages</button></li>
</ul>
</div>
);

class Insights extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  render() {
    return (
      <div className={s.container}>
        <Menu />
        <div className={s.main}>
          { this.props.insights.map(insight => (<Insight insight={insight}/>))}
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string)
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  insights: selectors.selectInsights,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Insights);
