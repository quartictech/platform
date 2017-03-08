import * as React from "react";
import { connect } from "react-redux";

import { IDataset } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetInfo } from "../../components/DatasetInfo";

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

const Insight = (props) => (
  <div className="pt-card pt-elevation-2 pt-interactive">
    We have discovered a fabulous insight!
    {props.wat}
  </div>
);

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
        <div className={s.main}>
          <Insight/>
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
