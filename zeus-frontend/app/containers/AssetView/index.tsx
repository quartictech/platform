import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

interface IProps {
  ui: any;
}

interface IState {
  datasetId: string;
};

class AssetView extends React.Component<IProps, IState> {
  public state : IState = {
    datasetId: null,
  };

  render() {
    return (
      <div className={s.container}>
        Asset view
             </div>
    );
  }
}

export { AssetView };

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string)
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AssetView);
