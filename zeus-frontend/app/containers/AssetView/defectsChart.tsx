import * as React from "react";
import {
  NonIdealState,
  Position,
} from "@blueprintjs/core";
import { TimeChart } from "../../components/TimeChart";
import NormalPicker from "../../components/NormalPicker";
import Pane from "../../components/Pane";
import { Asset, TimeSeriesPoint, MaintenanceEvent } from "../../models";

interface DefectsChartProps {
  asset: Asset;
}

interface State {
  seriesSelection: string;
}

class DefectsChart extends React.Component<DefectsChartProps, State> {
  constructor(props: DefectsChartProps) {
    super(props);
    this.state = {
      seriesSelection: null,
    };
  }

  public componentWillReceiveProps(nextProps: DefectsChartProps) {
    if (!this.state.seriesSelection &&
      nextProps.asset.data &&
      nextProps.asset.data._defect_time_series) {
      const timeSeriesKeys = Object.keys(nextProps.asset.data._defect_time_series);
      if (timeSeriesKeys.length > 0) {
        this.setState({ seriesSelection: timeSeriesKeys[0] });
      }
    }
  }

  render() {
    const timeSeries = this.computeTimeSeries(this.props.asset);
    const events = this.computeEvents(this.props.asset);
    return (
      <Pane
        title="Defects vs. time"
        iconName="error"
        extraHeaderContent={this.props.asset._defect_time_series ? this.renderChartButtons(this.props.asset) : null}
      >
        { this.props.asset._defect_time_series ?
          <TimeChart
            yLabel={this.state.seriesSelection}
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

  private computeTimeSeries(asset): TimeSeriesPoint[] {
    if (this.state.seriesSelection) {
      return asset._defect_time_series[this.state.seriesSelection]
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

  private renderChartButtons(asset) {
    const charts = Object.keys(asset._defect_time_series);
    return (
      <NormalPicker
        iconName="timeline-line-chart"
        position={Position.TOP}
        selected={this.state.seriesSelection}
        entries={charts}
        onChange={id => this.setState({ seriesSelection: id })}
      />
    );
  }
};

export default DefectsChart;