import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import {
  Classes,
  NonIdealState,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import insights, { Insight } from "./insights";

interface Props {
  params: {
    insightName: string;
  };
}

const UnknownInsight: React.SFC<{}> = () => (
  <NonIdealState
    visual="error"
    title="Unknown insight."
  />
);

class InsightView extends React.Component<Props, {}> {
  render() {
    const insight = insights[this.props.params.insightName] || {
      componentClass: UnknownInsight,
      title: "<< Unknown insight >>",
      disabled: false,
    } as Insight;

    return (
      <DocumentTitle title={`Quartic - ${insight.title}`}>
        <div style={{ width: "100%" }}>
          {this.maybeDescription(insight)}
          {React.createElement(insight.componentClass)}
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


export default InsightView;
