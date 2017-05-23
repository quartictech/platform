import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as moment from "moment";
import * as numeral from "numeraljs";
import * as classNames from "classnames";
import * as _ from "underscore";
import { TimeSeriesPoint, MaintenanceEvent } from "../../models";

import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

import {
  Classes,
  Intent,
  NonIdealState,
  Position,
  Spinner,
  Tag,
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
        title="Defects"
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

  private renderStat(key, value, quartile) {
    let color = null;
    let tag = null;
    if (quartile > 0.9) {
      color = "rgba(219, 55, 55, 0.4)";   // Alpha-modified version of callout with INTENT_DANGER
      tag = <Tag style={{ float: "right" }} intent={Intent.DANGER}>Top 10% offender</Tag>;
    } else if (quartile > 0.67) {
      color = "rgba(217, 130, 43, 0.4)";  // Alpha-modified version of callout with INTENT_WARNING
      tag = <Tag style={{ float: "right" }} intent={Intent.WARNING}>Top 33% offender</Tag>;
    } else if (quartile < 0.25) {
      color = "rgba(15, 153, 96, 0.4)";  // Alpha-modified version of callout with INTENT_SUCCESS
      tag = <Tag style={{ float: "right" }} intent={Intent.SUCCESS}>Bottom 25% offender</Tag>;
    }
    return (
      <tr className={classNames(Classes.CALLOUT)} style={{ backgroundColor: color }}>
        <td className={s["attribute-name"]}>{key}</td>
        <td>{value} {tag}</td>
      </tr>
    );
  }

  private renderAttributes(asset) {
    return (
      <div>
        <div className={classNames(Classes.CALLOUT)} style={{ margin: "10px" }}>
          <h1>{asset.RSL}</h1>
          <table className={classNames(Classes.TABLE, Classes.TABLE_CONDENSED)} style={{ width: "100%"}}>
            <tbody>
              {
                _.map({
                  "Road name": toTitleCase(asset["Road Name"]),
                  "Section description": toTitleCase(asset["Section Description"]),
                  "Link place": `${asset["Link"]} ${asset["Place"]}`,
                  "Length (m)": numeral(asset["Length"]).format("0.00"),
                  "Speed limit (mph)": asset["Speed Limit"],
                }, (v, k: string) => <tr key={k}><td className={s["attribute-name"]}>{k}</td><td>{v}</td></tr>)
              }
              {_.map(asset._stats, (v, k) => this.renderStat(k, numeral(v[0]).format("0.00"), v[1]))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  private renderEventRow(idx, event) {
    if (event.type === "job" || event.type === "job_geo") {
      return (
        <tr key={idx}>
          <td>
              { event.type === "job" ? <span className="pt-icon-standard pt-icon-person"></span> :
                <span className="pt-icon-standard pt-icon-geosearch"></span> } {event["Number"]}
          </td>

          <td>
            {event._date ? moment(event._date).format("Do MMM YYYY") : null}
          </td>

          <td>
            { event["Type"] }
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
            {event._date ? moment(event._date).format("Do MMM YYYY") : null}
          </td>

          <td>
            {event["Treatment"]} <br />
          </td>

          <td>
            {event["Notes"]}
          </td>
        </tr>);
    }
  }

  private renderEventsTable(asset) {
    const treatments = asset._treatments
      .map(t => Object.assign(t, { type: "treatment", _date: t["Estimated Completion Date"] }));

    const jobs = asset._jobs
      .map(j => Object.assign(j, { type: "job", _date: j["Start Date"] }));

    const jobsGeo = asset._jobs_geo
      .map(j => Object.assign(j, { type: "job_geo", _date: j["Start Date"]}));

    const events = treatments.concat(jobs).concat(jobsGeo);

    events.sort((a, b) => a._date - b._date);

    return (
      <Pane title="Events" iconName="timeline-events">
        <table
          className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
          style={{ width: "100%", tableLayout: "fixed" }}
        >
          <thead>
            <tr>
              <th width={120}>Job No.</th>
              <th width={120}>Date</th>
              <th width={200}>Type</th>
              <th className={s.eventsTableDetailsColumns}>Details</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e, idx) => this.renderEventRow(idx, e))}
          </tbody>
        </table>
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
      <DocumentTitle title={`Quartic - ${this.props.params.assetId}`}>
        <div className={s.container}>
          {this.renderData()}
        </div>
      </DocumentTitle>
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
  mapDispatchToProps,
)(AssetView);
