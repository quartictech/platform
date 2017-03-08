import * as React from "react";
import { connect } from "react-redux";

import { IDataset } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

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

class Inventory extends React.Component<IProps, IState> {
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
        Sweet inventory
      </div>
    );
  }
}

export { Inventory };

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
)(Inventory);
