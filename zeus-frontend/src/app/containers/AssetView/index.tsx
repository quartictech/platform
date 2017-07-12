import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import Attributes from "./attributes";
import PreviewMap from "./previewMap";
import DefectsChart from "./defectsChart";
import Schematic from "./schematic";
import EventsTable from "./eventsTable";

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
  asset,
} from "../../api";

import * as selectors from "../../redux/selectors";
import { Asset } from "../../models";
const s = require("./style.css");

interface Props {
  params: {
    assetId: string;
  };

  asset: ResourceState<Asset>;
  assetRequired: (string) => void;
}

interface State {
  yearSelection: string;
}

class AssetView extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      yearSelection: "2016",
    };
  }

  public componentWillMount() {
    this.onNewAsset(this.props.params.assetId);
  }

  public componentWillReceiveProps(nextProps: Props) {
    if (this.props.params.assetId !== nextProps.params.assetId) {
      this.onNewAsset(nextProps.params.assetId);
    }
  }

  private onNewAsset(assetId: string) {
    this.props.assetRequired(assetId);
  }

  private renderData() {
    const asset = this.props.asset;
    switch (asset.status) {
      case ResourceStatus.LOADED:
        return (
          <div style={{flex: 1}}>
            <div className={s.splitRow}>
              <div className={s.splitLeft}>
                <Attributes asset={asset.data} />
              </div>
              <div className={s.splitRight}>
                <PreviewMap asset={asset.data} />
              </div>
            </div>
            <DefectsChart
              asset={asset.data}
              onSelectYear={year => this.setState({ yearSelection: year })}
            />
            <Schematic
              asset={asset.data}
              yearSelection={this.state.yearSelection}
              onSelectYear={year => this.setState({ yearSelection: year })}
            />
            <EventsTable asset={asset.data} />
          </div>
        );

      case ResourceStatus.NOT_LOADED:
        return <NonIdealState
          visual="cross"
          title="No asset loaded."
        />;

      case ResourceStatus.LOADING:
        return <NonIdealState
          visual={<Spinner className={Classes.LARGE} />}
          title="Loading asset ..."
        />;

      case ResourceStatus.ERROR:
        return <NonIdealState
          visual="error"
          title="There was an error loading asset."
        />;
    }
  }

  render() {
    return (
      <DocumentTitle title={`Quartic - ${this.props.params.assetId}`}>
        <div className={s.container}>
          {this.renderData()}
        </div>
      </DocumentTitle>
    );
  }
}

const mapDispatchToProps = {
  assetRequired: resourceActions(asset).requiredFresh,
};

const mapStateToProps = createStructuredSelector({
  asset: selectors.selectAsset,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(AssetView);
