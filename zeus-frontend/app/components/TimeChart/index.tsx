import * as React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as Plottable from "plottable";
import * as _ from "underscore";
import * as classNames from "classnames";

const s = require("./style.css");

import { MaintenanceEvent, TimeSeriesPoint } from "../../models";

import SizeMe from "react-sizeme";

interface IProps {
  events: MaintenanceEvent[];
  timeSeries: { [id: string]: TimeSeriesPoint[]};
  colors: { [id: string]: string};
  yLabel: string;
}

interface IState {
  tooltip: {
    x: number;
    y: number;
    value: string;
  };
}

const EVENT_TYPES = ["maintenance", "failure", "other"];

function setEq(a: any[], b: any[]) {
  const sB = new Set(b);

  if (a.length !== b.length) {
    return false;
  }
  return a.every(x => sB.has(x));
}


class RealTimeChart extends React.Component<IProps, IState> {
  private timeSeriesDatasets: { [id: string]: Plottable.Dataset};
  private eventsDatasets: { [id: string]: Plottable.Dataset};
  private plots: Plottable.Plot[];
  private chart: Plottable.Component;
  private pzi: Plottable.Interactions.PanZoom;
  private group: Plottable.Components.Group;
  private xScale: Plottable.Scales.Time;
  private yAxisLabel: Plottable.Components.AxisLabel;
  private yScaleTimeSeries: Plottable.Scales.Linear;

  constructor() {
    super();
    this.timeSeriesDatasets = {};
    this.eventsDatasets = EVENT_TYPES.reduce((obj, k) =>
      Object.assign(obj, { [k]: new Plottable.Dataset() }), {});
    this.plots = [];

    this.createChart();

    this.state = {
      tooltip: null,
    };
  }

  updateChart(timeSeries) {
    _.each(this.plots, p => this.group.remove(p));

    this.timeSeriesDatasets = Object.keys(timeSeries).reduce((obj, k) =>
      Object.assign(obj, {[k]: new Plottable.Dataset()}), {});

    const timeSeriesPlots = Object.keys(timeSeries)
      .map(k => new Plottable.Plots.Line()
        .attr("stroke", _ => this.props.colors[k])
        .x(d => d.x, this.xScale)
        .y(d => d.y, this.yScaleTimeSeries)
        .addDataset(this.timeSeriesDatasets[k]));

    const timeSeriesScatterPlots = Object.keys(timeSeries)
      .map(k => new Plottable.Plots.Scatter()
        .attr("stroke", _ => this.props.colors[k])
        .x(d => d.x, this.xScale)
        .y(d => d.y, this.yScaleTimeSeries)
        .addDataset(this.timeSeriesDatasets[k]));

    let plots: Plottable.Plot[] = timeSeriesPlots;
    plots = plots.concat(timeSeriesScatterPlots);
    plots.forEach(p => {
      this.pzi.attachTo(p);
      this.group.append(p);
    });

    this.plots = plots;
  }

  createChart() {
    this.xScale = new Plottable.Scales.Time();
    const yScale = new Plottable.Scales.Linear();
    yScale.range([0, 1]);
    const xAxis = new Plottable.Axes.Time(this.xScale, "bottom");
    const colorScale = new Plottable.Scales.Color();
    colorScale.domain(EVENT_TYPES);
    colorScale.range(["#0f9960", "#db1e7b", "#ff6600"]);
    const gridLines = new Plottable.Components.Gridlines(this.xScale, null);

    this.yAxisLabel = new Plottable.Components.AxisLabel("", 270);
    this.yScaleTimeSeries = new Plottable.Scales.Linear();
    this.yScaleTimeSeries.domainMin(-0.1);
    const yAxis = new Plottable.Axes.Numeric(this.yScaleTimeSeries, "left");

    const eventsPlot = new Plottable.Plots.Segment()
       .attr("stroke", function(_d, _i, dataset) { return dataset.metadata(); }, colorScale)
       .x(function(d: MaintenanceEvent) { return d.timestamp; }, this.xScale)
       .y(function(_) { return 0; }, yScale)
       .x2((d: MaintenanceEvent) => d.timestamp)
       .y2( _ => 1)
       .datasets(_.values(this.eventsDatasets));

    let plots: Plottable.Component[] = [gridLines, eventsPlot];
    this.group = new Plottable.Components.Group(plots);
    this.pzi = new Plottable.Interactions.PanZoom();
    this.pzi.addXScale(this.xScale);
    this.pzi.attachTo(eventsPlot);
    window.addEventListener("resize", function () {
      eventsPlot.redraw();
    });

    const pointer = new Plottable.Interactions.Pointer();
    pointer.onPointerMove(p => {
      const closest = eventsPlot.entityNearest(p);
      if (closest) {
        this.setState({
          tooltip: {
            x: closest.position.x,
            y: closest.position.y,
            value: closest.datum.detail,
          },
        });
      }
    });
    pointer.onPointerExit(() => this.setState({ tooltip: null }));

    pointer.attachTo(eventsPlot);

    this.chart = new Plottable.Components.Table([
      [this.yAxisLabel, yAxis, this.group],
      [null, null, xAxis],
    ]);
  }

  render() {
    if (this.chart) {
      this.chart.redraw();
    }
    // overflow:hidden required due to https://github.com/palantir/plottable/issues/3298
    return (
      <div style={{padding: "10px", width: "99%"}}>
        <div className={s.chart} style={{ width: "100%", height: 175, overflow: "hidden" }} ref="svg"/>
        <div
          style={{ visibility: this.state.tooltip ? "visible" : "hidden", display: "inline-block" }}
          className={classNames(Classes.CALLOUT, Classes.INTENT_PRIMARY)}
        >
          <b>Selection:</b> {this.state.tooltip ? this.state.tooltip.value : "\u00A0"}
        </div>
      </div>
    );
  }

  componentDidMount() {
    this.chart.renderTo(this.refs["svg"] as HTMLElement);
  }

  componentWillUpdate(nextProps: IProps) {
    if (!setEq(Object.keys(nextProps.timeSeries), Object.keys(this.timeSeriesDatasets))) {
      this.updateChart(nextProps.timeSeries);
    }

    Object.keys(nextProps.timeSeries).forEach(k =>
      this.timeSeriesDatasets[k].data(nextProps.timeSeries[k]));

    if (this.yAxisLabel) {
      this.yAxisLabel.text(nextProps.yLabel);
    }

    EVENT_TYPES.forEach(k =>
      this.eventsDatasets[k].data(nextProps.events.filter(e => e.type === k)).metadata(k));
  }
}

export const TimeChart = SizeMe<IProps>()(RealTimeChart); // tslint:disable-line:variable-name
