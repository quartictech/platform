import * as React from "react";

import * as Plottable from "plottable";
const s = require("./style.css");

import SizeMe from "react-sizeme";

class RealTimeChart  extends React.Component<any, any> {
  maintenance = [{ x: new Date(2017, 1, 1) }, { x: new Date(2017, 2, 3) }];
  failures = [{ x: new Date(2017, 4, 1) }];

  constructor() {
    super();
  }

  createChart() {
    const xScale = new Plottable.Scales.Time();
    const yScale = new Plottable.Scales.Linear();
    yScale.range([0, 1]);
    const xAxis = new Plottable.Axes.Time(xScale, "bottom");
    var colorScale = new Plottable.Scales.Color();
    colorScale.domain(["maintenance", "failure"])
    colorScale.range(['#1F77B4', '#FF7F0E']);
    const gridLines = new Plottable.Components.Gridlines(xScale, null);

    const plot = new Plottable.Plots.Segment()
      .addDataset(new Plottable.Dataset(this.maintenance).metadata("maintenance"))
      .addDataset(new Plottable.Dataset(this.failures).metadata("failure"))
       .attr("stroke", function(_d, _i, dataset) { return dataset.metadata(); }, colorScale)
       .x(function(d) { return d.x; }, xScale)
       .y(function(_) { return 0; }, yScale)
       .x2(d => d.x)
       .y2( _ => 1);
    const chart = new Plottable.Components.Group([plot, gridLines]);
    let pzi = new Plottable.Interactions.PanZoom();
    pzi.addXScale(xScale);
    pzi.attachTo(plot);
    window.addEventListener("resize", function() {
      plot.redraw();
    });

    //const legend = new Plottable.Components.Legend(colorScale).xAlignment("left").maxEntriesPerRow(3);
    //const group = new Plottable.Components.Group([yAxis, plot]);
    this.state = {
      chart: new Plottable.Components.Table([
      [chart],
      [xAxis],
    ]),
    plot: plot
    };
  }

  render() {
    return (
      <div style={{padding: "10px", width: "99%"}}>
    <div className={s.chart} style={{width: "100%", height: 100}} ref="svg">
    </div>
    </div>
    );
  }

  componentDidMount() {
    this.createChart();
    this.state.chart.renderTo(this.refs["svg"]);
    this.state.plot.redraw();
  }
}

export const TimeChart = SizeMe()(RealTimeChart);
