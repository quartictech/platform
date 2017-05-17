import * as React from "react";

import * as Plottable from "plottable";
const s = require("./style.css");

import { MaintenanceEvent, TimeSeriesPoint } from "../../models";

import SizeMe from "react-sizeme";

interface ITimeChartProps {
  events: MaintenanceEvent[];
  timeSeries: TimeSeriesPoint[];
  yLabel: string;
}

class RealTimeChart extends React.Component<ITimeChartProps, any> {
  constructor() {
    super();
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
      .addDataset(new Plottable.Dataset(this.props.timeSeries))
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

    // const legend = new Plottable.Components.Legend(colorScale).xAlignment("left").maxEntriesPerRow(3);
    // const group = new Plottable.Components.Group([yAxis, plot]);
    this.state = {
      chart: new Plottable.Components.Table([
      [yLabel, chart],
      [null, xAxis],
    ]),
    plot: plot
    };
  }

  render() {
    return (
      <div style={{padding: "10px", width: "99%"}}>
    <svg className={s.chart} style={{width: "100%", height: 150}} ref="svg">
    </svg>
    </div>
    );
  }

  componentDidMount() {
    this.createChart();
    this.state.chart.renderTo(this.refs["svg"]);
    this.state.plot.redraw();
  }
}

declare function SizeMe<T>(): (c: React.ComponentClass<T>) => React.ComponentClass<T>
export const TimeChart = SizeMe<ITimeChartProps>()(RealTimeChart); // tslint:disable-line:variable-name
