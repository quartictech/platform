import * as React from "react";
import {
  NonIdealState,
  Position,
  Button,
  Classes,
} from "@blueprintjs/core";

import * as naturalsort from "javascript-natural-sort";

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
    return (
      <Pane
        title="Defects vs. time"
        iconName="error"
        extraHeaderContent={this.props.asset._defect_time_series ? this.renderChartButtons(this.props.asset) : null}
      >
        {(this.props.asset._defect_time_series && this.state.seriesSelection) ? this.chart() : this.nonIdeal()}
      </Pane>
    );
  }

  private chart() {
    const events = this.computeEvents(this.props.asset);
    const defectTimeSeries = this.props.asset._defect_time_series ?
      this.props.asset._defect_time_series[this.state.seriesSelection] :
      null;

    const series = {
      defects: {
        points: this.computeTimeSeries(defectTimeSeries),
        color: "#1f77b4",
      },
      ...(this.state.predictions && { predictions: {
        points: this.computeTimeSeries(this.props.asset._defect_predictions),
        color: "#00FF00",
      }}),
      ...(this.state.predictions && { usefulPredictions: {
        points: this.computeTimeSeries(this.props.asset._defect_predictions_future),
        color: "#00FF00",
        dashed: true,
      }}),
    };

    return (
      <TimeChart
        yLabel={this.state.seriesSelection}
        events={events}
        series={series}
        onSelectYear={this.props.onSelectYear}
      />
    );
  }

  private nonIdeal() {
    return <NonIdealState visual="info" title="No survey data available" />;
  }

  private computeTimeSeries(ts): TimeSeriesPoint[] {
    if (ts) {
      return ts.series.map(({ timestamp, value }) => ({ x: new Date(timestamp), y: value }));
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
        job["Type"] !== "MNHL - Highway Lighting" &&
        !job["Type"].startsWith("Street Cleansing") &&
        !job["Type"].startsWith("Street Lighting") &&
        !job["Type"].startsWith("Drain Cleansing") &&
        !job["Type"].startsWith("Drainage") &&
        !job["Type"].startsWith("Highway Signs"),
      )
      .map(job => ({
        type: "other",
        detail: `${job["Type"]} (${job["Number"]})`,
        timestamp: new Date(job["Start Date"]),
      }));

    return treatments.concat(jobs);
  }

  private renderChartButtons(asset) {
    const charts = Object.keys(asset._defect_time_series).sort(naturalsort);
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
          position={Position.TOP_RIGHT}
          selected={this.state.seriesSelection}
          entries={charts}
          onChange={id => this.setState({ seriesSelection: id })}
        />
      </div>
    );
  }
}

export default DefectsChart;
