import React from "react";
import {
  Position,
} from "@blueprintjs/core";
import SizeMe from "react-sizeme";
import * as Plottable from "plottable";
import "plottable/plottable.css";
import NormalPicker from "../NormalPicker";
import Pane from "../Pane";
import styles from "./styles.css";

class Chart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { selectedAttribute: undefined };
    this.createChart();

    this.onAttributeChange = this.onAttributeChange.bind(this);
  }

  createChart() {
    const xScale = new Plottable.Scales.Time().domain([new Date(2000, 0, 1), new Date(2016, 11, 31)]);
    const yScale = new Plottable.Scales.Linear();
    const xAxis = new Plottable.Axes.Time(xScale, "bottom").yAlignment("center");
    const yAxis = new Plottable.Axes.Numeric(yScale, "left");

    this.colorScale = new Plottable.Scales.Color();
    this.plot = new Plottable.Plots.Line()
      .x(d => new Date(d.timestamp), xScale)
      .y(d => d.value, yScale);
    this.plot.attr("stroke", (d, i, dataset) => dataset.metadata().name, this.colorScale);
    this.plot.autorangeMode("x");

    const legend = new Plottable.Components.Legend(this.colorScale).xAlignment("left").maxEntriesPerRow(3);
    const group = new Plottable.Components.Group([this.plot, yAxis]);
    this.chart = new Plottable.Components.Table([
      [legend],
      [group],
      [xAxis],
    ]);
  }

  componentDidMount() {
    this.chart.renderTo("svg#example");
    window.addEventListener("resize", () => this.chart.redraw());
  }

  getAttributes = (timeSeries) => (timeSeries !== undefined ? Object.keys(timeSeries) : []);

  updateChart(data) {
    this.colorScale.domain(Object.keys(data));
    const datasets = Object.keys(data)
      .map(k => {
        const plottableData = data[k].series.sort((a, b) => a.timestamp - b.timestamp);
        return new Plottable.Dataset(plottableData, { "name": k });
      });
    this.plot.datasets(datasets);
  }

  componentWillReceiveProps(nextProps) {
    const attributes = this.getAttributes(nextProps.timeSeries);

    if (attributes.length === 0) {
      this.setState({ selectedAttribute: undefined });
    } else if (this.state.selectedAttribute === undefined) {
      this.setState({ selectedAttribute: attributes[0] });
    }
  }

  componentWillUpdate(nextProps, nextState) {
    let nextData = {};

    if (nextProps.timeSeries !== undefined &&
        nextState.selectedAttribute !== undefined &&
        nextState.selectedAttribute in nextProps.timeSeries) {
      nextData = nextProps.timeSeries[nextState.selectedAttribute];
    }

    this.updateChart(nextData);
  }

  onAttributeChange(value) {
    this.setState({ selectedAttribute: value });
  }

  render() {
    if (this.chart) {
      this.chart.redraw();
    }
    return (
      <Pane
        title="Chart"
        iconName="chart"
        visible={this.props.visible}
        extraHeaderContent={
          <NormalPicker
            iconName="timeline-line-chart"
            position={Position.TOP}
            selected={this.state.selectedAttribute}
            entries={this.getAttributes(this.props.timeSeries)}
            onChange={this.onAttributeChange}
          />
        }
        onClose={() => this.props.onUiToggle("chart")}
      >
        <div className={styles.plotArea}>
          <svg id="example" />
        </div>
      </Pane>
    );
  }
}
export default SizeMe({ monitorHeight: true })(Chart);  // eslint-disable-line new-cap
