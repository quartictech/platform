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
import { Insight,  Asset } from "../../models";

import { Map } from "../../components";

interface IProps {
  ui: any;
  insights: [Insight];
  assets: { [id: string]: Asset };
  params: { 
    insightId: string;
  };
}

interface IState {
  datasetId: string;
};

const joinAssets = (insight: Insight, assets: {[id: string]: Asset}) => {
    return insight.assetIds.map(assetId => assets[assetId]);
};

const Asset = ({ insight, asset }) => {
  const failed = insight.unfailedAssetIds.indexOf(asset.id) === -1;
    // HACK: I'm filtering out failures here because the data is all fake
    return (
    <div key={asset.id} className={classNames(s.subCard, "pt-card", "pt-elevation-2")}>
            <h5>
              <Link to={`/assets/${asset.id}`}>
                {asset.clazz}-{asset.model.manufacturer}{asset.model.name}-{asset.serial}
              </Link>
            </h5>
              { failed ? <span className="pt-tag pt-intent-danger" style={{float: "right"}}>Failed</span> :
          <span className="pt-tag pt-intent-success" style={{ float: "right" }}>No failure</span> }
            <TimeChart
              yLabel="Voltage"
              events={failed ? asset.events : asset.events.filter(ev => ev.type !== "failure")}
            />
          </div>);
};

class InsightView extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  render() {
    const insight = this.props.insights.filter(i => i.id === this.props.params.insightId)[0];
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
      <Map
        height={100}
        locations={assets.map((asset) => asset.location)}
        colors={assets.map(asset => insight.unfailedAssetIds.indexOf(asset.id) > -1 ? 1 : 0)}
      />
      </div>

        { assets.map(asset => <Asset key={asset.id} asset={asset} insight={insight} /> )}
      </div>

      <div className={s.right}>
        <div className="pt-card pt-elevation-2">
          <h2>Actions</h2>
          <p>
          4 / 10 of similar assets have not yet failed. 
          </p>
          <p><b>Consider scheduling proactive maintenance.</b></p>
          <Link
            className="pt-button pt-intent-primary"
            to={{ pathname: "/inventory", query: {clazz: "Signal"}}}
          >
            View
          </Link>
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
