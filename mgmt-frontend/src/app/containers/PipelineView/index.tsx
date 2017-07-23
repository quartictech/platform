import * as React from "react";
import { connect } from "react-redux";
import * as cytoscape from "cytoscape";
import * as cytoscapeDagre from "cytoscape-dagre";
cytoscapeDagre(cytoscape);

import { DatasetMap, Ui } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

const pipeline = require("./pipeline.json");

interface IProps {
  datasets: DatasetMap;
  ui: Ui;
  createDataset: (any) => any;
  fetchDatasets: any;
  closeNewDatasetModal: any;
  deleteDataset: (string) => void;
}

class PipelineView extends React.Component<IProps, {}> {
  render() {
    return (
      <div className={s.container}>
        <div id="cy" className={s.cy}/>
      </div>
    );
  }

  componentDidMount() {
    cytoscape({
      container: document.getElementById("cy"),
      elements: pipeline,
      layout: {
        name: "dagre",
        rankDir: "UD",
      },
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
            "shape": "rectangle",
            "background-color": "#db1e7b",
          },
        },
        {
          selector: "edge",
          style: {
            "width": 2,
            "line-color": "#ccc",
            "target-arrow-color": "#ccc",
            "target-arrow-shape": "triangle",
          },
        },
      ],
    });

  }
}

const mapDispatchToProps = {
  fetchDatasets: actions.fetchDatasets,
  createDataset: actions.createDataset,
  deleteDataset: actions.deleteDataset,
  closeNewDatasetModal: () => actions.setActiveModal(null as string),
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
  ui: selectors.selectUi,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(PipelineView);
