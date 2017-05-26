import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import Attributes from "./attributes";
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
import * as actions from "../../redux/actions";
import { Asset } from "../../models";
const s = require("./style.css");

import { Map } from "../../components/Map";
import Pane from "../../components/Pane";

interface IProps {
  ui: any;
  params: {
    assetId: string;
  };

  asset: ResourceState<Asset>;
  assetRequired: (string) => void;
}

class AssetView extends React.Component<IProps, {}> {
  constructor() {
    super();
  }

  public componentWillMount() {
    this.onNewAsset(this.props.params.assetId);
  }

  public componentWillReceiveProps(nextProps: IProps) {
    if (this.props.params.assetId !== nextProps.params.assetId) {
      this.onNewAsset(nextProps.params.assetId);
    }
  }

  private onNewAsset(assetId: string) {
    this.props.assetRequired(assetId);
    this.setState({ defectChartSelection: null });
  }

  private renderMap(asset) {
    const fc: GeoJSON.FeatureCollection<GeoJSON.LineString> = {
      type: "FeatureCollection",
      features: [
        {
          type: "Feature",
          geometry: asset._geometry,
          properties: {},
        },
      ],
    };
    return (
      <Pane>
        <Map height={100} width={500} featureCollection={fc}/>
      </Pane>
    );
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
                {this.renderMap(asset.data)}
              </div>
            </div>
            <DefectsChart asset={asset.data} />
            <Schematic asset={asset.data} />
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
  closeNewDatasetModal: () => actions.setActiveModal(null as string),
  assetRequired: resourceActions(asset).required,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  asset: selectors.selectAsset,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(AssetView);
