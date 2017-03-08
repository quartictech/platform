import * as React from "react";
import { connect } from "react-redux";

import { IDataset } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetList } from "../../components/DatasetList";
import { DatasetInfo } from "../../components/DatasetInfo";
import { NewDataset } from "../../components/NewDataset";

interface IProps {
  datasets: { [id: string]: IDataset };
  ui: any;
  createDataset: (any) => any;
  fetchDatasets: any;
  closeNewDatasetModal: any;
  deleteDataset: (string) => void;
}

interface IState {
  datasetId: string;
};

class Insights extends React.Component<IProps, IState> {
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
        <h1>Insights</h1>
        <NewDataset
          visible={this.props.ui.activeModal === "newDataset"}
          createDataset={this.props.createDataset}
          closeNewDatasetClick={this.props.closeNewDatasetModal}
        />
        <div className={s.main}>
          <DatasetList
            searchString={this.props.ui.searchString}
            datasets={this.props.datasets}
            selectedId={this.state.datasetId}
            onSelect={this.selectDataset}
          />
        </div>

        {
          (this.state.datasetId === null)
            ? null
            : (
              <div className={s.right}>
                <DatasetInfo
                  id={this.state.datasetId}
                  dataset={this.props.datasets[this.state.datasetId]}
                  deleteClick={this.props.deleteDataset}
                />
              </div>
            )
        }
      </div>
    );
  }

  private selectDataset = (id: string) => {
    this.setState({datasetId: id});
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
)(Insights);
