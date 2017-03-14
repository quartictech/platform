import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { IInsight, IAsset } from "../../models";
import * as classNames from "classnames";
const s = require("./style.css");
import { BarChart } from "../../components/BarChart";
import { TimeChart } from "../../components/TimeChart";
import * as _ from "underscore";

import { Link } from "react-router";

interface IProps {
  ui: any;
  insights:IInsight[];
  assets: {[id:string]:IAsset}
  location?: {
    query?: {
      insightType: string
    }
  };
}

interface IState {
};

const Insight = ({ insight, assets }) => (
  <div className={classNames(s.insight, "pt-card", "pt-elevation-2", "pt-interactive")}>
    <div className="pt-callout pt-icon-warning-sign" style={{backgroundColor: "#ffffff"}}>
     <h5>{ insight.title }</h5>
           <p>
           { insight.body }
           </p>
           </div>
    <div>
      { insight.insightType === "failure"? <div><TimeChart events={assets[_.first(insight.assetIds) as string].events} /></div>: null }
      { insight.barChart ? (<div>
        <BarChart data={insight.barChart} xLabel={insight.barChartXLabel} />
      </div>) : null}
      <Link className="pt-button pt-intent-primary" to={`/insights/${insight.id}`}>
      See detail
      </Link>
    </div>
  </div>
);

class Insights extends React.Component<IProps, IState> {
  public state : IState = {
  };

  render() {
    return (
      <div className={s.container}>
          { this.props.insights.filter (i => (!this.props.location.query.insightType ||
            i.insightType === this.props.location.query.insightType))
            .map(insight => (<Insight key={insight.id} insight={insight} assets={this.props.assets} />))}
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
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Insights);
