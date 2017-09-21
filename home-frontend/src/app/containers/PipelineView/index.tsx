import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import { connect } from "react-redux";
import { Link } from "react-router";
import { Classes, IconClasses, Spinner } from "@blueprintjs/core";
import * as classNames from "classnames";
import * as cytoscape from "cytoscape";
import * as cytoscapeDagre from "cytoscape-dagre";
cytoscapeDagre(cytoscape);

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import { PipelineState, PipelineStatus } from "../../redux/reducers/pipeline";
import * as actions from "../../redux/actions";
const s = require("./style.css");

// TODO - can be make this pure-render?

interface IProps {
  pipeline: PipelineState;
  fetchPipeline: any;
  params: {
    build: number;
  };
}

class PipelineView extends React.Component<IProps, {}> {
  private cy;

  // TODO - change title here
  render() {
    return (
      <DocumentTitle title="Quartic - Pipeline">
        <div className={s.container}>
          <div className={s.main}>
            {this.renderPipeline()}
          </div>
        </div>
      </DocumentTitle>
    );
  }

  renderPipeline() {
    switch (this.props.pipeline.status) {
      case PipelineStatus.LOADED:
        return <div id="cy" className={s.cy}/>;
      case PipelineStatus.LOADING:
        return <Spinner className={Classes.LARGE} />;
      case PipelineStatus.NOT_FOUND:
        return this.noBuildFound();
      default: return null;
    }
  }

  noBuildFound = () => {
    const message = this.props.params.build ?
      `Build #${this.props.params.build} did not generate a valid DAG.` :
      `There are no valid DAGs yet for this project.`;

    return (
      <div className={Classes.NON_IDEAL_STATE}>
        <div className={classNames(Classes.NON_IDEAL_STATE_VISUAL, Classes.NON_IDEAL_STATE_ICON)}>
          <span className={classNames(Classes.ICON, IconClasses.WARNING_SIGN)}/>
        </div>
        <h4 className={Classes.NON_IDEAL_STATE_TITLE}>{message}</h4>
        <div className={Classes.NON_IDEAL_STATE_DESCRIPTION}>
          Fix your DAG or view the <Link to="/pipeline/">latest version</Link>.
        </div>
      </div>
    );
  }

  componentDidMount() {
    this.props.fetchPipeline(this.props.params.build);
  }

  componentDidUpdate(prevProps: IProps) {
    if (prevProps.params.build !== this.props.params.build) {
      this.props.fetchPipeline(this.props.params.build);
    }

    if (prevProps !== this.props) {
      if (this.props.pipeline.status === PipelineStatus.LOADED) {
        this.cy = this.configureCytoscape();
        this.cy.elements().remove();
        this.cy.add(this.props.pipeline.data);
        this.cy.elements().layout({
          name: "dagre",
          rankDir: "TB",
          fit: true,
        }).run();
      }
    }
  }

  private configureCytoscape() {
    const cy = cytoscape({
      container: document.getElementById("cy"),
      elements: {},
      autoungrabify: true,
      style: [
        {
          selector: "node",
          style: {
            "label": "data(title)",
            "width": "label",
            "padding": "5px",
            "color": "#ffffff",
            "font-size": "7px",
            "text-valign": "center",
            "shape": "roundrectangle",
            "background-opacity": 0.7,
            "background-color": ele => ele.data("type") === "derived" ? "#db1e7b" : "#1c6a9d",
            "border-width": "5px",
            "border-color": ele => ele.data("type") === "derived" ? "#db1e7b" : "#1c6a9d",
          },
        },
        {
          selector: "edge",
          style: {
            "width": 2,
            "line-color": "#ccc",
            "target-arrow-color": "#ccc",
            "target-arrow-shape": "triangle",
            "curve-style": "unbundled-bezier",
            "control-point-distances": "20 -20",
            "control-point-weights": "0.25 0.75",
          },
        },
      ],
    });

    cy.on("select", "node", (_) => {
      cy.$("node").css({
        "background-opacity": 0.7,
      });
      cy.$("node:selected").css({
        "background-opacity": 1.0,
      });
      cy.$("edge").css({
        "line-color": "#ccc",
        "target-arrow-color": "#ccc",
      });
      cy.$("node:selected").connectedEdges().css({
        "line-color": "#db1e7b",
        "target-arrow-color": "#db1e7b",
      });
    });

    return cy;
  }
}

const mapDispatchToProps = {
  fetchPipeline: actions.fetchPipeline,
};

const mapStateToProps = createStructuredSelector({
  pipeline: selectors.selectPipeline,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(PipelineView);
