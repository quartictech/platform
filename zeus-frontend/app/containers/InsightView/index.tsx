import * as React from "react";
import { connect } from "react-redux";

import { IDataset } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import * as classNames from "classnames";
const s = require("./style.css");


interface IProps {
  datasets: { [id: string]: IDataset };
  ui: any;
  createDataset: (any) => any;
  fetchDatasets: any;
  closeNewDatasetModal: any;
  deleteDataset: (string) => void;
  params: { [id: string]: any };
}

interface IState {
  datasetId: string;
};

class InsightView extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  componentDidMount() {
    this.props.fetchDatasets();
  }

  componentWillReceiveProps(props: IProps) {
      if (! (this.state.datasetId in props.datasets)) {
          this.setState({ datasetId: null });
      }
  }

  render() {
    return (
      <div className={s.container}>
          <div className={classNames(s.main, "pt-card", "pt-elevation-2")}>
            <h2>{this.props.params["insightId"]} Repeated failures in asset class: <a href="#">Boiler-5000</a></h2>
            <hr/>
            Hello
          </div>
      </div>
    );
  }
}

const mapDispatchToProps = {
  fetchDatasets: actions.fetchDatasets,
  createDataset: actions.createDataset,
  deleteDataset: actions.deleteDataset,
  closeNewDatasetModal: () => actions.setActiveModal(null as string)
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
  ui: selectors.selectUi
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(InsightView);
