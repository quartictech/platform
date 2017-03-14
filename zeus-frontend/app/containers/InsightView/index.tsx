import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as classNames from "classnames";
const s = require("./style.css");

import { Link } from "react-router";

import { TimeChart } from "../../components";
import { Classes } from "@blueprintjs/core";
import { IInsight } from "../../models";

interface IProps {
  ui: any;
  insights: [IInsight];
  params: { 
    insightId: string;
  }
}

interface IState {
  datasetId: string;
};

class InsightView extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  render() {
    const insight = this.props.insights.filter(i => i.id == this.props.params.insightId)[0];
    return (
<div className={s.container}>
  <div className={s.main}>
    <div className={classNames(s.card, "pt-card", "pt-elevation-2")}>
      <h2>{insight.title} <small>#{this.props.params.insightId}</small></h2>

      <div className={s.subInsightContainer}>
        <div className={classNames(s.subInsight, "pt-callout", "pt-intent-danger", "pt-icon-info-sign")}>
          This machinery is dangerously noob.
        </div>
        <div className={classNames(s.subInsight, "pt-callout", "pt-intent-danger", "pt-icon-info-sign")}>
          This machinery is dangerously noob.
        </div>
      </div>

        <div className={s.plotControls}>
        <label className="pt-label pt-inline" style={{marginBottom: 0}}>
              Time Series
              <div className={Classes.SELECT}>
                  <select> 
                    <option key={0} value="temperature">Temperature</option>
                    <option key={1} value="pressure">Pressure</option>
                  </select>
              </div>
         </label>
         </div>
        </div>

        <div className={classNames(s.subCard, "pt-card", "pt-elevation-2")}>
          <h5>
            <Link to={`/assets/X-192`}>Boiler-X192</Link>
          </h5>
          <TimeChart />
        </div>

        <div className={classNames(s.subCard, "pt-card", "pt-elevation-2")}>
          <h5>Bolier-X184</h5>
          <TimeChart />
        </div>
      </div>

      <div className={s.right}>
        <div className="pt-card pt-elevation-2">
          <h2>Actions</h2>
          <p>
          11 / 50 of similar assets have not yet failed.
          </p>
          <button className="pt-button pt-intent-primary">View</button>
        </div>
      </div>
    </div>
    );
  }
}

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  insights: selectors.selectInsights,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(InsightView);
