import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import { connect } from "react-redux";

import { IDatasetCoords, DatasetMap } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetInfo } from "../../components/DatasetInfo";

interface IProps {
  fetchDatasets: any;
  datasets: DatasetMap;
  deleteDataset: (string) => void;
  params: IDatasetCoords;
}

class DatasetView extends React.Component<IProps, {}> {

  componentDidMount() {
    this.props.fetchDatasets();
  }

  render() {
    const dataset = this.props.datasets[this.props.params.namespace][this.props.params.id];
    return (
      <DocumentTitle title={`Quartic - ${dataset.metadata.name}`}>
        <div className={s.center}>
          <DatasetInfo
            coords={this.props.params}
            dataset={dataset}
            deleteClick={this.props.deleteDataset}
          />
        </div>
      </DocumentTitle>
    );
  }
}

export { DatasetView };

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
)(DatasetView);
