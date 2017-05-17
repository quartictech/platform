import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";

import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

import {
  Classes,
  NonIdealState,
  Spinner,
} from "@blueprintjs/core";

import {
  asset
} from "../../api";

import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { Asset } from "../../models";
const s = require("./style.css");

interface IProps {
  ui: any;
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

  renderData() {
    const asset = this.props.asset;
    switch (asset.status) {
      case ResourceStatus.LOADED:
        return (
          <h1>{asset.data.clazz}-{asset.data.model.manufacturer}-{asset.data.model.name}-{asset.data.serial}</h1>
        );

      case ResourceStatus.NOT_LOADED:
        return <NonIdealState
          visual="cross"
          title="No assets loaded."
        />;

      case ResourceStatus.LOADING:
        return <NonIdealState
          visual={<Spinner className={Classes.LARGE} />}
          title="Loading assets ..."
        />;

      case ResourceStatus.ERROR:
        return <NonIdealState
          visual="error"
          title="There was an error loading assets."
        />;
    }
  }

  render() {
    return (
      <div className={s.container}>
      {this.renderData()}
      </div>
    );
  }
}

export { AssetView };

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string),
  assetRequired: resourceActions(asset).required,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  asset: selectors.selectAsset,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AssetView);
