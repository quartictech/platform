import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
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
        <div className={classNames(s.card, "pt-card", "pt-elevation-2")}>
            <h2>Insight #{this.props.params.insightId}: {insight.title}</h2>

            <label className="pt-label pt-inline">
        Time Series
        <div className={Classes.SELECT}>
            <select> 
               <option key={0} value="temperature">Temperature</option>
               <option key={1} value="pressure">Pressure</option>
            </select>
        </div>
</label>
        </div>

          <div className={classNames(s.subCard, "pt-card", "pt-elevation-2")}>
            <h5><Link to={`/assets/X-192`}>Boiler-X192</Link></h5>
            <TimeChart />
          </div>

          <div className={classNames(s.subCard, "pt-card", "pt-elevation-2")}>
            <h5>Bolier-X184</h5>
            <TimeChart />
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
)(InsightView);
