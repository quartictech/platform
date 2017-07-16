import * as React from "react";

import * as Plottable from "plottable";
const s = require("./style.css");

import { Bar } from "../../models";

import SizeMe from "react-sizeme";

interface IProps {
  data: Bar[];
  xLabel: string;
  yLabel: string;
}

class RealBarChart  extends React.Component<IProps, any> {
  createChart() {
    const xScale = new Plottable.Scales.Linear();
    const yScale = new Plottable.Scales.Category();
    const xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    const yAxis = new Plottable.Axes.Category(yScale, "left");

    const xLabel = new Plottable.Components.AxisLabel(this.props.xLabel, 0);
    const yLabel = new Plottable.Components.AxisLabel(this.props.yLabel, 270);


    const plot = new Plottable.Plots.Bar("horizontal")
      .addDataset(new Plottable.Dataset(this.props.data))
       .x(d => d.value, xScale)
       .y(d => d.name, yScale);
    const chart = new Plottable.Components.Group([plot]);
    window.addEventListener("resize", () => plot.redraw());

    // const legend = new Plottable.Components.Legend(colorScale).xAlignment("left").maxEntriesPerRow(3);
    // const group = new Plottable.Components.Group([yAxis, plot]);
    this.state = {
      plot,
      chart: new Plottable.Components.Table([
        [yLabel, yAxis, chart],
        [null, null, xAxis],
        [null, null, xLabel],
      ]),
    };
  }

  render() {
    return (
      <div style={{ padding: "10px", width: "99%" }}>
        <div className={s.chart} style={{ width: "100%", height: "200px" }} ref="svg" />
      </div>
    );
  }

  componentDidMount() {
    this.createChart();
    this.state.chart.renderTo(this.refs["svg"]);
    this.state.plot.redraw();
  }
}

export const BarChart = SizeMe<IProps>()(RealBarChart); // tslint:disable-line:variable-name
