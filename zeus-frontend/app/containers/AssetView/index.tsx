import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";

import {
  resourceActions,
  ResourceState,
  // ResourceStatus,
} from "../../api-management";

import {
  asset
} from "../../api";

import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { Asset } from "../../models";
const s = require("./style.css");

interface IProps {
  ui: any;
  // assets: {[id: string]: Asset};
  params: {
    assetId: string;
  };

  asset: ResourceState<Asset>;
  assetRequired: (string) => void;
}

class AssetView extends React.Component<IProps, void> {
  componentDidMount() {
    this.props.assetRequired(this.props.params.assetId);
  }

  render() {
    // const asset = this.props.assets[this.props.params.assetId];
    return (
      <div className={s.container}>
        {/*<h1>{asset.clazz}-{asset.model.manufacturer}-{asset.model.name}-{asset.serial}</h1>*/}
      </div>
    );
  }
}

export { AssetView };

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string),
  assetRequired: resourceActions(asset).required
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AssetView);
