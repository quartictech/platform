import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import {
  Classes,
  NonIdealState,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";

import insights, { InsightType } from "./insights";
import * as selectors from "../../redux/selectors";
import {
  resourceActions,
  ResourceState,
} from "../../api-management";

import {
  insight,
} from "../../api";

import {
  Insight,
} from "../../models";

interface Props {
  insight: ResourceState<Insight>;
  params: {
    insightName: string;
  };
  insightRequired: (string) => void;
}

// tslint:disable-next-line:variable-name
const UnknownInsight: React.SFC<{}> = () => (
  <NonIdealState
    visual="error"
    title="Unknown insight."
  />
);

class InsightView extends React.Component<Props, {}> {
  public componentWillMount() {
    this.onNewInsight(this.props.params.insightName);
  }

  private onNewInsight(insightName: string) {
    this.props.insightRequired(insightName);
  }

  public componentWillReceiveProps(nextProps: Props) {
    if (this.props.params.insightName !== nextProps.params.insightName) {
      this.onNewInsight(nextProps.params.insightName);
    }
  }

  render() {
    const insight = this.props.insight;
    const insightType = insights[this.props.params.insightName] || {
      componentClass: UnknownInsight,
      title: "<< Unknown insight >>",
      disabled: false,
    } as InsightType;

    return (
      <DocumentTitle title={`Quartic - ${insightType.title}`}>
        <div style={{ width: "100%" }}>
          {this.maybeDescription(insightType)}
          {React.createElement(insightType.componentClass, insight.data)}
        </div>
      </DocumentTitle>
    );
  }

  private maybeDescription(insight: Insight) {
    if (!insight.description) {
      return null;
    }

    return (
      <div className={classNames(Classes.CALLOUT, Classes.iconClass("info-sign"), Classes.INTENT_PRIMARY)}>
        <h5>{insight.title}</h5>
        {insight.description || null}
      </div>
    );
  }
}

const mapDispatchToProps = {
  insightRequired: resourceActions(insight).requiredFresh,
};

const mapStateToProps = createStructuredSelector({
  insight: selectors.selectInsight,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(InsightView);
