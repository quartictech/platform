import * as React from "react";

import * as Plottable from "plottable";
const s = require("./style.css");

import { MaintenanceEvent, TimeSeriesPoint } from "../../models";

import SizeMe from "react-sizeme";

interface IProps {
  events: MaintenanceEvent[];
  timeSeries: TimeSeriesPoint[];
  yLabel: string;
}

interface IState {
  timeSeriesPlot: Plottable.Plots.Line<{}>;
  chart: any;
}

class RealTimeChart extends React.Component<IProps, IState> {
  constructor() {
    super();
    this.state = {
      timeSeriesPlot: null,
      chart: null
    };
  }

  createChart() {
    const xScale = new Plottable.Scales.Time();
    const yScale = new Plottable.Scales.Linear();
    yScale.range([0, 1]);
    const xAxis = new Plottable.Axes.Time(xScale, "bottom");
    const colorScale = new Plottable.Scales.Color();
    colorScale.domain(["maintenance", "failure"]);
    colorScale.range(["#1F77B4", "#db1e7b"]);
    const gridLines = new Plottable.Components.Gridlines(xScale, null);

    const yLabel = new Plottable.Components.AxisLabel(this.props.yLabel, 270);

    const yScaleTimeSeries = new Plottable.Scales.Linear();

    const maintenance = this.props.events.filter(event => event.type === "maintenance");
    const failures = this.props.events.filter(event => event.type === "failure");
    const plot = new Plottable.Plots.Segment()
      .addDataset(new Plottable.Dataset(maintenance).metadata("maintenance"))
      .addDataset(new Plottable.Dataset(failures).metadata("failure"))
       .attr("stroke", function(_d, _i, dataset) { return dataset.metadata(); }, colorScale)
       .x(function(d) { return d.date; }, xScale)
       .y(function(_) { return 0; }, yScale)
       .x2(d => d.date)
       .y2( _ => 1);

    const timeSeriesPlot = new Plottable.Plots.Line()
      .attr("stroke", _ => "#D3D3D3")
      .x(d => d.x, xScale)
      .y(d => d.y, yScaleTimeSeries);

    const chart = new Plottable.Components.Group([timeSeriesPlot, plot, gridLines]);
    let pzi = new Plottable.Interactions.PanZoom();
    pzi.addXScale(xScale);
    pzi.attachTo(plot);
    pzi.attachTo(timeSeriesPlot);
    window.addEventListener("resize", function() {
      plot.redraw();
    });

    this.state = {
      chart: new Plottable.Components.Table([
        [yLabel, chart],
        [null, xAxis],
      ]),
      timeSeriesPlot: timeSeriesPlot
    };
  }

  render() {
    if (this.state.chart) {
      this.state.chart.redraw();
    }
    return (
      <div style={{padding: "10px", width: "99%"}}>
        <svg className={s.chart} style={{ width: "100%", height: 150 }} ref="svg">
        </svg>
      </div>
    );
  }

  componentDidMount() {
    this.createChart();
    this.state.chart.renderTo(this.refs["svg"]);
    this.state.chart.redraw();
  }

  componentWillUpdate(nextProps, _) {
    if (this.state.timeSeriesPlot != null) {
      this.state.timeSeriesPlot.datasets([new Plottable.Dataset(nextProps.timeSeries)]);
    }
  }
}

declare function SizeMe<T>(): (c: React.ComponentClass<T>) => React.ComponentClass<T>;
export const TimeChart = SizeMe<IProps>()(RealTimeChart); // tslint:disable-line:variable-name
