import * as React from "react";
import { connect } from "react-redux";

// import * as Blueprint from "@blueprintjs/core";
// const { Menu, MenuItem, MenuDivider } = Blueprint;

import { IDataset } from "../../models";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetList } from "../../components/DatasetList";

interface IProps {
  datasets: { [id: string]: IDataset };
  fetchDatasets: any;
}

interface IState {
  datasetId: string;
};

class Home extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  componentDidMount() {
    this.props.fetchDatasets();
  }

  render() {
    return (
      <div className={s.container}>
        <div className={s.main}>
          <DatasetList
            datasets={this.props.datasets}
            onSelect={this.selectDataset}
          />
        </div>

        <div className={s.right}>
          <pre>
            {JSON.stringify(this.props.datasets[this.state.datasetId], null, '  ')}
          </pre>
        </div>
      </div>
    );
  }

  private selectDataset = (id: string) => {
    this.setState({datasetId: id});
  }
}

export { Home };

const mapDispatchToProps = {
  fetchDatasets: actions.fetchDatasets
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Home);
