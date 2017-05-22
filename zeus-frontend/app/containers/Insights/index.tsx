import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { Insight, Asset } from "../../models";
const s = require("./style.css");

import * as classNames from "classnames";

import { Link } from "react-router";

import { InsightSummary } from "../../components";

interface IProps {
  ui: any;
  insights: Insight[];
  assets: {[id: string] : Asset};
  location?: {
    query?: {
      insightType: string;
    };
  };
}

interface IState {
}

interface IInsightProps {
  insight: Insight;
  assets: {[id: string] : Asset};
}

const InsightDetail = (props: IInsightProps) => (
  <div className={classNames(s.insight, "pt-card", "pt-elevation-2", "pt-interactive")}>
    <div className="pt-callout pt-icon-warning-sign" style={{backgroundColor: "#ffffff"}}>
     <h5>{ props.insight.title }</h5>
        { props.insight.assetClass
          ? <div className="pt-tag" style={{ float: "right" }}>{props.insight.assetClass}</div>
          : null
        }
    </div>
    <div>
      <InsightSummary key={props.insight.id} insight={props.insight} assets={props.assets} />
    </div>
    <Link className="pt-button pt-intent-primary" to={`/insights/${props.insight.id}`}>
      See detail
    </Link>
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
            .map(insight => (<InsightDetail key={insight.id} insight={insight} assets={this.props.assets} />))}
      </div>
    );
  }
}

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string),
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  insights: selectors.selectInsights,
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(Insights);
