import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { IAsset } from "../../models";
const s = require("./style.css");

interface IProps {
  ui: any;
  assets: {[id:string]: IAsset};
  params: {
    assetId: string;
  };
}

class AssetView extends React.Component<IProps, void> {
  render() {
    const asset = this.props.assets[this.props.params.assetId];
    return (
      <div className={s.container}>
        <h1>{asset.clazz}-{asset.model.manufacturer}-{asset.model.name}-{asset.serial}</h1>
      </div>
    );
  }
}

export { AssetView };

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string)
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AssetView);
