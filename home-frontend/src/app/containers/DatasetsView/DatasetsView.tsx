import * as React from "react";
import { connect } from "react-redux";

import { IDatasetCoords, DatasetMap, Ui } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetList } from "../../components/DatasetList";
import { DatasetInfo } from "../../components/DatasetInfo";
import { NewDataset } from "../../components/NewDataset";

interface IProps {
  datasets: DatasetMap;
  ui: Ui;
  createDataset: (any) => any;
  fetchDatasets: any;
  closeNewDatasetModal: any;
  showNewDatasetModal: any;
  deleteDataset: (string) => void;
}

interface IState {
  datasetCoords: IDatasetCoords;
}

class DatasetsView extends React.Component<IProps, IState> {
  public state : IState = {
    datasetCoords: null,
  };

  componentDidMount() {
    this.props.fetchDatasets();
  }

  componentWillReceiveProps(props: IProps) {
    if (this.state.datasetCoords) {
      const ns = this.state.datasetCoords.namespace;
      const id = this.state.datasetCoords.id;
      if (!(ns in props.datasets) || !(id in props.datasets[ns])) {
        this.setState({ datasetCoords: null });
      }
    }
  }

  render() {
    return (
      <div className={s.container}>
        <NewDataset
          visible={this.props.ui.activeModal === "newDataset"}
          createDataset={this.props.createDataset}
          closeNewDatasetClick={this.props.closeNewDatasetModal}
        />
        <div className={s.main}>
          <div className={s.center}>
            <DatasetList
              searchString={this.props.ui.searchString}
              datasets={this.props.datasets}
              onSelect={this.selectDataset}
              selected={this.state.datasetCoords}
              selectedNamespace={this.props.ui.namespace}
              showNewDatasetModal={this.props.showNewDatasetModal}
            />
          </div>

        {this.maybeDatasetInfo()}
        </div>
      </div>
    );
  }

  private maybeDatasetInfo() {
    if (this.state.datasetCoords === null) {
      return null;
    }

    return (
      <div className={s.right}>
        <DatasetInfo
          coords={this.state.datasetCoords}
          dataset={this.props.datasets[this.state.datasetCoords.namespace][this.state.datasetCoords.id]}
          deleteClick={this.props.deleteDataset}
        />
      </div>
    );
  }

  private selectDataset = (coords: IDatasetCoords) => {
    this.setState({ datasetCoords: coords });
  }
}

export { DatasetsView };

const mapDispatchToProps = {
  showNewDatasetModal: () => actions.setActiveModal("newDataset"),
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
)(DatasetsView);
