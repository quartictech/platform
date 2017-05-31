import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import {
  NonIdealState,
} from "@blueprintjs/core";
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
        {React.createElement(insight.componentClass)}
      </DocumentTitle>
    );
  }
}


export default InsightView;
