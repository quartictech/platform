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
  timeSeriesScatterPlot: Plottable.Plots.Scatter<{}, {}>;
  eventsPlot: Plottable.Plots.Segment<{}, {}>;
  chart: any;
}

class RealTimeChart extends React.Component<IProps, IState> {
  constructor() {
    super();
    this.state = {
      timeSeriesPlot: null,
      timeSeriesScatterPlot: null,
      eventsPlot: null,
      chart: null,
    };
  }

  createChart() {
    const xScale = new Plottable.Scales.Time();
    const yScale = new Plottable.Scales.Linear();
    yScale.range([0, 1]);
    const xAxis = new Plottable.Axes.Time(xScale, "bottom");
    const colorScale = new Plottable.Scales.Color();
    colorScale.domain(["maintenance", "failure", "other"]);
    colorScale.range(["#0f9960", "#db1e7b", "#ff6600"]);
    const gridLines = new Plottable.Components.Gridlines(xScale, null);

    const yLabel = new Plottable.Components.AxisLabel(this.props.yLabel, 270);

    const yScaleTimeSeries = new Plottable.Scales.Linear();
    const yAxis = new Plottable.Axes.Numeric(yScaleTimeSeries, "left");

    const eventsPlot = new Plottable.Plots.Segment()
       .attr("stroke", function(_d, _i, dataset) { return dataset.metadata(); }, colorScale)
       .x(function(d: MaintenanceEvent) { return d.timestamp; }, xScale)
       .y(function(_) { return 0; }, yScale)
       .x2((d: MaintenanceEvent) => d.timestamp)
       .y2( _ => 1);

    const timeSeriesPlot = new Plottable.Plots.Line()
      .attr("stroke", _ => "#1f77b4")
      .x(d => d.x, xScale)
      .y(d => d.y, yScaleTimeSeries);
    const timeSeriesScatterPlot = new Plottable.Plots.Scatter()
      .attr("stroke", _ => "#1f77b4")
      .x(d => d.x, xScale)
      .y(d => d.y, yScaleTimeSeries);


    const chart = new Plottable.Components.Group([timeSeriesPlot, timeSeriesScatterPlot, eventsPlot, gridLines]);
    let pzi = new Plottable.Interactions.PanZoom();
    pzi.addXScale(xScale);
    pzi.attachTo(eventsPlot);
    pzi.attachTo(timeSeriesPlot);
    window.addEventListener("resize", function() {
      eventsPlot.redraw();
    });

    this.state = {
      chart: new Plottable.Components.Table([
        [yLabel, yAxis, chart],
        [null, null, xAxis],
      ]),
      eventsPlot,
      timeSeriesPlot,
      timeSeriesScatterPlot,
    };
  }

  render() {
    if (this.state.chart) {
      this.state.chart.redraw();
    }
    return (
      <div style={{padding: "10px", width: "99%"}}>
        <svg className={s.chart} style={{ width: "100%", height: 175 }} ref="svg">
        </svg>
      </div>
    );
  }

  componentDidMount() {
    this.createChart();
    this.state.chart.renderTo(this.refs["svg"]);
  }

  componentWillUpdate(nextProps: IProps, _) {
    if (this.state.timeSeriesPlot != null) {
      this.state.timeSeriesPlot.datasets([new Plottable.Dataset(nextProps.timeSeries)]);
      this.state.timeSeriesScatterPlot.datasets([new Plottable.Dataset(nextProps.timeSeries)]);
    }

    if (this.state.eventsPlot != null) {
      this.state.eventsPlot.datasets([
        new Plottable.Dataset(nextProps.events.filter(e => e.type === "maintenance")).metadata("maintenance"),
        new Plottable.Dataset(nextProps.events.filter(e => e.type === "failure")).metadata("failure"),
        new Plottable.Dataset(nextProps.events.filter(e => e.type === "other")).metadata("other"),
      ]);
    }
  }
}

export const TimeChart = SizeMe<IProps>()(RealTimeChart); // tslint:disable-line:variable-name
