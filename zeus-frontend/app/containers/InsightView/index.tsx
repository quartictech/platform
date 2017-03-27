import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as classNames from "classnames";
const s = require("./style.css");

import { Link } from "react-router";

import { TimeChart } from "../../components";
import { InsightSummary } from "../../components";
// import { Classes } from "@blueprintjs/core";
import { IInsight,  IAsset } from "../../models";

import { Map } from "../../components";

interface IProps {
  ui: any;
  insights: [IInsight];
  assets: {[id:string]: IAsset};
  params: { 
    insightId: string;
  }
}

interface IState {
  datasetId: string;
};

const joinAssets = (insight: IInsight, assets: {[id: string]: IAsset}) => {
    return insight.assetIds.map(assetId => assets[assetId]);
}

class InsightView extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  render() {
    const insight = this.props.insights.filter(i => i.id == this.props.params.insightId)[0];
    const assets = joinAssets(insight, this.props.assets);
    return (
<div className={s.container}>
  <div className={s.main}>
    <div className={classNames(s.card, "pt-card", "pt-elevation-2")}>
      <h2>{insight.title} <small>#{this.props.params.insightId}</small></h2>
      <div className={s.subInsightContainer}>
        { insight.subInsights ? insight.subInsights.map((sub, idx) => 
        <div key={idx} className={classNames(s.subInsight, "pt-callout", "pt-intent-danger", sub.icon)}>
          {sub.text}
        </div>) : null }
      </div>

      <InsightSummary insight={insight} assets={this.props.assets}/>
        {/*<div className={s.plotControls}>
        <label className="pt-label pt-inline" style={{marginBottom: 0}}>
              Time Series
              <div className={Classes.SELECT}>
                  <select> 
                    <option key={0} value="temperature">Temperature</option>
                    <option key={1} value="pressure">Pressure</option>
                  </select>
              </div>
         </label>
         </div>*/}
        </div>

     <div className={classNames(s.card, "pt-card", "pt-elevation-2")}>
      <Map height={100} locations={assets.map((asset) => asset.location)} />
      </div>

        { assets.map(asset => (
    <div key={asset.id} className={classNames(s.subCard, "pt-card", "pt-elevation-2")}>
            <h5>
              <Link to={`/assets/${asset.id}`}>{asset.clazz}-{asset.model.manufacturer}{asset.model.name}-{asset.serial}</Link>
            </h5>
            <TimeChart events={asset.events}/>
          </div>

        ))}
      </div>

      <div className={s.right}>
        <div className="pt-card pt-elevation-2">
          <h2>Actions</h2>
          <p>
          4 / 10 of similar assets have not yet failed. 
          </p>
          <p><b>Consider scheduling proactive maintenance.</b></p>
          <Link className="pt-button pt-intent-primary"  to={{ pathname: "/inventory", query: {clazz: "Boiler"}}}>View</Link>
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
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(InsightView);
