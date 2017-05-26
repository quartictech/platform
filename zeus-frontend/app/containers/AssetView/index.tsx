import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import { TimeSeriesPoint, MaintenanceEvent } from "../../models";
import Attributes from "./attributes";
import Schematic from "./schematic";
import EventsTable from "./events";

import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

import {
  Classes,
  NonIdealState,
  Position,
  Spinner,
} from "@blueprintjs/core";

import {
  asset,
} from "../../api";

import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { Asset } from "../../models";
const s = require("./style.css");

import { TimeChart } from "../../components/TimeChart";
import { Map } from "../../components/Map";
import NormalPicker from "../../components/NormalPicker";
import Pane from "../../components/Pane";

interface IProps {
  ui: any;
  params: {
    assetId: string;
  };

  asset: ResourceState<Asset>;
  assetRequired: (string) => void;
}

interface IState {
  defectChartSelection: string;
}

class AssetView extends React.Component<IProps, IState> {
  constructor() {
    super();
    this.state = {
      defectChartSelection: null,
    };
  }

  public componentWillMount() {
    this.onNewAsset(this.props.params.assetId);
  }

  public componentWillReceiveProps(nextProps: IProps) {
    if (this.props.params.assetId !== nextProps.params.assetId) {
      this.onNewAsset(nextProps.params.assetId);
    }

    if (!this.state.defectChartSelection &&
      nextProps.asset.data &&
      nextProps.asset.data._defect_time_series) {
      const timeSeriesKeys = Object.keys(nextProps.asset.data._defect_time_series);
      if (timeSeriesKeys.length > 0) {
        this.setState({ defectChartSelection: timeSeriesKeys[0] });
      }
    }
  }

  private onNewAsset(assetId: string) {
    this.props.assetRequired(assetId);
    this.setState({ defectChartSelection: null });
  }


  private computeTimeSeries(asset): TimeSeriesPoint[] {
    if (this.state.defectChartSelection) {
      return asset._defect_time_series[this.state.defectChartSelection]
        .series.map( ({ timestamp, value }) => ({x: new Date(timestamp), y: value }));
    }
    return [];
  }

  private computeEvents(asset): MaintenanceEvent[] {
    const treatments = asset._treatments
      .filter(job => job["Estimated Completion Date"] != null)
      .map(job => ({
        type: "maintenance",
        detail: job["Treatment"],
        timestamp: new Date(job["Estimated Completion Date"]),
      }));

    const jobs = asset._jobs.concat(asset._jobs_geo)
      .filter(job => job["Start Date"] != null)
      .filter(job =>
          job["Type"] !== "Street Cleansing - Reactive Response" &&
          job["Type"] !== "MNHL - Highway Lighting" &&
          job["Type"] !== "Street Lighting - Reactive Fault Repairs")
      .map(job => ({
        type: "other",
        detail: `${job["Type"]} (${job["Number"]})`,
        timestamp: new Date(job["Start Date"]),
      }));

    return treatments.concat(jobs);
  }

  private renderDefectsChart(asset) {
    const timeSeries = this.computeTimeSeries(asset);
    const events = this.computeEvents(asset);
    return (
      <Pane
        title="Defects vs. time"
        iconName="error"
        extraHeaderContent={asset._defect_time_series ? this.renderChartButtons(asset) : null}
      >
        { asset._defect_time_series ?
          <TimeChart
            yLabel={this.state.defectChartSelection}
            events={events}
            timeSeries={timeSeries}
          /> :
          <NonIdealState
            visual="info"
            title="No survey data available"
          />
         }
      </Pane>
    );
  }

  private renderChartButtons(asset) {
    const charts = Object.keys(asset._defect_time_series);
    return (
      <NormalPicker
        iconName="timeline-line-chart"
        position={Position.TOP}
        selected={this.state.defectChartSelection}
        entries={charts}
        onChange={id => this.setState({ defectChartSelection: id })}
      />
    );
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
            {this.renderDefectsChart(asset.data)}
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
