import * as React from "react";

import * as Plottable from "plottable";
const s = require("./style.css");

interface IBar {
  name: string;
  value: number;
};

interface IProps {
  data: IBar[];
};

export class BarChart  extends React.Component<IProps, any> {
  createChart() {
    const xScale = new Plottable.Scales.Linear();
    const yScale = new Plottable.Scales.Category();
    const xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    const yAxis = new Plottable.Axes.Category(yScale, "left");


    const plot = new Plottable.Plots.Bar("horizontal")
      .addDataset(new Plottable.Dataset(this.props.data))
       .x(function(d) { return d.value; }, xScale)
       .y(function(d) { return d.name; }, yScale);
    const chart = new Plottable.Components.Group([plot]);
    window.addEventListener("resize", function() {
      plot.redraw();
    });

    //const legend = new Plottable.Components.Legend(colorScale).xAlignment("left").maxEntriesPerRow(3);
    //const group = new Plottable.Components.Group([yAxis, plot]);
    this.state = {
      chart: new Plottable.Components.Table([
      [yAxis, chart],
      [null, xAxis],
    ]),
    plot: plot
    };
  }

  render() {
    return (
      <div style={{padding: "10px", width: "99%"}}>
    <svg className={s.chart} style={{width: "100%"}} ref="svg">
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
