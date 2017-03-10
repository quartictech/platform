import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { IInsight } from "../../models";
import * as classNames from "classnames";
const s = require("./style.css");
import { BarChart } from "../../components/BarChart";

import { Link } from "react-router";

interface IProps {
  ui: any;
  insights:IInsight[];
  location?: {
    query?: {
      insightType: string
    }
  };

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
      { insight.barChart ? (<div>
        <BarChart data={insight.barChart}/>
      </div>) : null}
      <Link className="pt-button pt-intent-primary" to={`/insights/${insight.id}`}>
      See detail
      </Link>
    </div>
  </div>
);



class Insights extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  render() {
    return (
      <div className={s.container}>
          { this.props.insights.filter (i => (!this.props.location.query.insightType ||
            i.insightType === this.props.location.query.insightType)).map(insight => (<Insight key={insight.id} insight={insight}/>))}
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
