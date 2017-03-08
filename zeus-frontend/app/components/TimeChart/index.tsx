import * as React from "react";

import * as Plottable from "plottable";
const s = require("./style.css");

export class TimeChart  extends React.Component<any, any> {
  maintenance = [{ x: new Date(2017, 1, 1) }, { x: new Date(2017, 1, 3) }];
  failures = [{ x: new Date(2017, 4, 1) }];

  constructor() {
    super();
  }

  createChart() {
    const xScale = new Plottable.Scales.Time();
    const yScale = new Plottable.Scales.Linear();
    yScale.range([0, 1]);
    const xAxis = new Plottable.Axes.Time(xScale, "bottom");

    const plot = new Plottable.Plots.Segment()
      .addDataset(new Plottable.Dataset(this.maintenance))
      .addDataset(new Plottable.Dataset(this.failures))
       .x(function(d) { return d.x; }, xScale)
       .y(function(_) { return 0; }, yScale)
       .x2(d => d.x)
       .y2( _ => 1);
    let pzi = new Plottable.Interactions.PanZoom();
    pzi.addXScale(xScale);
    pzi.attachTo(plot);

    //const legend = new Plottable.Components.Legend(colorScale).xAlignment("left").maxEntriesPerRow(3);
    //const group = new Plottable.Components.Group([yAxis, plot]);
    return new Plottable.Components.Table([
      [plot],
      [xAxis],
    ]);
  }

  render() {
    return (
      <div style={{padding: "10px", width: "95%"}}>
    <svg className={s.chart} style={{width: "100%"}} ref="svg">
    </svg>
    </div>
    );
  }

  componentDidMount() {
    console.log("componentDidMount");
    this.state = { chart: this.createChart() };
    this.state.chart.renderTo(this.refs["svg"]);
  }
}
