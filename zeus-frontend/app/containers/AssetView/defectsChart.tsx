import * as React from "react";
import {
  NonIdealState,
  Position,
  Button,
  Classes,
} from "@blueprintjs/core";
import { TimeChart } from "../../components/TimeChart";
import NormalPicker from "../../components/NormalPicker";
import Pane from "../../components/Pane";
import { Asset, TimeSeriesPoint, MaintenanceEvent } from "../../models";

interface DefectsChartProps {
  asset: Asset;
  onSelectYear: (year: string) => void;
}

interface State {
  seriesSelection: string;
  predictions: boolean;
}

const DEFECT_CHART_DEFAULT_SELECTION = "Defects - CW / m";

class DefectsChart extends React.Component<DefectsChartProps, State> {
  constructor(props: DefectsChartProps) {
    super(props);
    this.state = {
      seriesSelection: null,
      predictions: false,
    };
  }

  public componentWillMount() {
    this.onNewStuff(this.props);
  }

  componentWillReceiveProps(nextProps: DefectsChartProps) {
    this.onNewStuff(nextProps);
  }

  private onNewStuff(props: DefectsChartProps) {
    if (!this.state.seriesSelection && props.asset && props.asset._defect_time_series) {
      if (DEFECT_CHART_DEFAULT_SELECTION in props.asset._defect_time_series) {
        this.setState({ seriesSelection: DEFECT_CHART_DEFAULT_SELECTION });
      } else {
        const timeSeriesKeys = Object.keys(props.asset._defect_time_series);
        if (timeSeriesKeys.length > 0) {
          this.setState({ seriesSelection: timeSeriesKeys[0] });
        }
      }
    }
  }

  render() {
    const events = this.computeEvents(this.props.asset);

    const timeSeries = Object.assign(
      {
      defects:
        this.computeTimeSeries(this.props.asset._defect_time_series[this.state.seriesSelection]),
      },
      this.state.predictions ? {
        predictions: this.computeTimeSeries(this.props.asset._defect_predictions),
      } : {},
    );

    return (
      <Pane
        title="Defects vs. time"
        iconName="error"
        extraHeaderContent={this.props.asset._defect_time_series ? this.renderChartButtons(this.props.asset) : null}
      >
        { (this.props.asset._defect_time_series && this.state.seriesSelection) ?
          <TimeChart
            yLabel={this.state.seriesSelection}
            events={events}
            timeSeries={timeSeries}
            colors={{ defects: "#1f77b4", predictions: "#00FF00" }}
            onSelectYear={this.props.onSelectYear}
          /> :
          <NonIdealState
            visual="info"
            title="No survey data available"
          />
          }
      </Pane>
    );
  }

  private computeTimeSeries(ts): TimeSeriesPoint[] {
    if (ts) {
      return ts
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
      <div>
        <Button
          className={Classes.MINIMAL}
          text="Predictions"
          iconName="chart"
          active={this.state.predictions}
          onClick={() => this.setState({ predictions: !this.state.predictions })}
        />
        <NormalPicker
          iconName="timeline-line-chart"
          position={Position.TOP}
          selected={this.state.seriesSelection}
          entries={charts}
          onChange={id => this.setState({ seriesSelection: id })}
        />
      </div>
    );
  }
}

export default DefectsChart;
