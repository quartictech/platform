import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import { connect } from "react-redux";

import { DatasetMap, Ui } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetList } from "../../components/DatasetList";
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
}

class DatasetsView extends React.Component<IProps, IState> {
  componentDidMount() {
    this.props.fetchDatasets();
  }


  render() {
    return (
      <DocumentTitle title="Quartic - Datasets">
        <div className={s.container}>
          <NewDataset
            visible={this.props.ui.activeModal === "newDataset"}
            createDataset={this.props.createDataset}
            closeNewDatasetClick={this.props.closeNewDatasetModal}
          />

          <div className={s.main}>
            <div className={s.center}>
              <DatasetList
                datasets={this.props.datasets}
                showNewDatasetModal={this.props.showNewDatasetModal}
              />
            </div>

          </div>
        </div>
      </DocumentTitle>
    );
  }
}

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
