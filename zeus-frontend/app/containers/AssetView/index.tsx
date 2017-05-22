import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";

import * as moment from "moment";
import * as numeral from "numeraljs";

import * as classNames from "classnames";

import { TimeSeriesPoint, MaintenanceEvent } from "../../models";

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

import { toTitleCase } from "../../helpers/Utils";
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

  componentDidMount() {
    this.props.assetRequired(this.props.params.assetId);
    document.title = `Quartic - ${this.props.params.assetId}`;
  }

  computeTimeSeries(asset): TimeSeriesPoint[] {
    if (this.state.defectChartSelection) {
      return asset._defect_time_series[this.state.defectChartSelection]
        .series.map( ({ timestamp, value }) => ({x: new Date(timestamp), y: value }));
    }
    return [];
  }

  computeEvents(asset): MaintenanceEvent[] {
    const treatments = asset._treatments
      .filter(job => job["Estimated Completion Date"] != null)
      .map(job => ({ type: "maintenance", timestamp: new Date(job["Estimated Completion Date"]) }));

    const jobs = asset._jobs
      .filter(job => job["Start Date"] != null)
      .map(job => ({ type: "other", timestamp: new Date(job["Start Date"]) }));

    return treatments.concat(jobs);
  }

  renderDefectsChart(asset) {
    const timeSeries = this.computeTimeSeries(asset);
    const events = this.computeEvents(asset);
    return (
      <Pane
        title="Defects"
        iconName="error"
        extraHeaderContent={this.renderChartButtons(asset)}
      >
        <TimeChart
          yLabel={this.state.defectChartSelection}
          events={events}
          timeSeries={timeSeries}
         />
      </Pane>
    );
  }

  renderChartButtons(asset) {
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

  renderMap(asset) {
    const fc: GeoJSON.FeatureCollection<GeoJSON.LineString> = {
      type: "FeatureCollection",
      features: [
        {
          type: "Feature",
          geometry: asset._geometry,
          properties: {}
        }
      ]
    };
    return (
      <Pane>
        <Map height={100} width={500} featureCollection={fc}/>
      </Pane>
    );
  }

  renderAttributes(asset) {
    return (
      <div className={classNames(Classes.CALLOUT)} style={{ margin: "10px" }}>
        <h1>{asset.RSL}</h1>
        <table className={classNames(Classes.TABLE, Classes.TABLE_CONDENSED)} style={{ width: "100%"}}>
          <tbody>
            <tr>
              <td>
                <b>Road Name</b>
              </td>
              <td>
                {toTitleCase(asset["Road Name"])}
              </td>
            </tr>
            <tr>
              <td>
                <b>Length (m)</b>
              </td>
              <td>
                { numeral(asset["Length"]).format("0.00") }
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    );
  }

  renderEventRow(idx, event) {
    if (event.type === "job") {
      return (
        <tr key={idx}>
          <td>
              <span className="pt-icon-standard pt-icon-person"></span> {event["Number"]}
          </td>

          <td>
            {event._date ? moment(event._date).format("Do MMMM YYYY, h:mm:ss a") : null}
          </td>

          <td>
            {event["Notes"]}
          </td>
        </tr>);
    } else if (event.type === "treatment") {
      return (
        <tr key={idx}>
          <td>
            <span className="pt-icon-standard pt-icon-tint"></span> {event["Confirm Number"]}
          </td>

          <td>
            {event._date ? moment(event._date).format("Do MMMM YYYY") : null}
          </td>

          <td>
            {event["Treatment"]} <br />
            {event["Notes"]}
          </td>
        </tr>);
    }
  }

  renderEventsTable(asset) {
    const events = asset._treatments
      .map(t => Object.assign(t, { type: "treatment", _date: t["Estimated Completion Date"] }))
      .concat(asset._jobs.map(j => Object.assign(j, { type: "job", _date: j["Actual Start Date"] })));

    events.sort((a, b) => a._date - b._date);

    return (
      <Pane title="Events" iconName="timeline-events">
        <table
          className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
          style={{ width: "100%" }}
        >
          <thead>
            <tr>
              <th width={100}>Job No.</th>
              <th>Date</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e, idx) => this.renderEventRow(idx, e))}
          </tbody>
        </table>
      </Pane>
    );
  }

  renderTreatmentsTable(asset) {
    return (
      <Pane title="Treatments" iconName="tint">
        <table
          className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
          style={{ width: "100%" }}
        >
          <thead>
            <tr>
              <th>Confirm Number</th>
              <th>Est. Completion Date</th>
              <th>Treatment Type</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            {asset._treatments
              .filter(job => job["Confirm Number"] ||
                job["Estimated Completion Date"] ||
                job["Treatment"] || job["Notes"])
              .map((job, idx) =>
              <tr key={idx}>
                <td>
                  {job["Confirm Number"]}
                </td>

                <td>
                  {job["Estimated Completion Date"] ?
                    moment(job["Estimated Completion Date"]).format("Do MMMM YYYY") : null}
                </td>

                <td>
                  {job["Treatment"]}
                </td>

                <td>
                  {job["Notes"]}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Pane>
    );
  }

  componentWillReceiveProps(nextProps) {
    if (!this.state.defectChartSelection &&
      nextProps.asset.data &&
      nextProps.asset.data._defect_time_series) {
      const timeSeriesKeys = Object.keys(nextProps.asset.data._defect_time_series);
      if (timeSeriesKeys.length > 0) {
        this.setState({ defectChartSelection: timeSeriesKeys[0] });
      }
    }
  }


  renderData() {
    const asset = this.props.asset;
    switch (asset.status) {
      case ResourceStatus.LOADED:
        return (
          <div style={{flex: 1}}>
            <div className={s.splitRow}>
              <div className={s.splitLeft}>
                {this.renderAttributes(asset.data)}
              </div>
              <div className={s.splitRight}>
                {this.renderMap(asset.data)}
              </div>
            </div>
            {this.renderDefectsChart(asset.data)}
            {this.renderEventsTable(asset.data)}
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
