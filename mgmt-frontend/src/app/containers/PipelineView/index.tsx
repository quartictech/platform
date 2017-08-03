import * as React from "react";
import { connect } from "react-redux";
import * as cytoscape from "cytoscape";
import * as cytoscapeDagre from "cytoscape-dagre";
cytoscapeDagre(cytoscape);

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

// TODO - can be make this pure-render?

interface IProps {
  pipeline: any;
  fetchPipeline: any;
}

class PipelineView extends React.Component<IProps, {}> {
  private cy;

  // TODO - change title here
  render() {
    return (
      <div className={s.container}>
        <div className={s.main}>
          <div id="cy" className={s.cy}/>
        </div>
      </div>
    );
  }

  componentDidMount() {
    this.cy = this.configureCytoscape();
    this.props.fetchPipeline();
  }

  componentDidUpdate(prevProps: IProps) {
    if (prevProps !== this.props) {
      this.cy.elements().remove();
      this.cy.add(this.props.pipeline);
      this.cy.elements().layout({
        name: "dagre",
        rankDir: "TB",
        fit: true,
      }).run();
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
