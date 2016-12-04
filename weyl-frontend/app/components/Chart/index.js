import React from "react";
import {
  Button,
  Classes,
} from "@blueprintjs/core";
import styles from "./styles.css";

import SizeMe from "react-sizeme";
import * as Plottable from "plottable";
import "plottable/plottable.css";
import classNames from "classnames";
import { Dropdown } from "semantic-ui-react";

const AttributePicker = ({ selected, attributes, onChange }) => {
  const options = attributes.map(attribute => ({ text: attribute, value: attribute }));
  return (
    <Dropdown
      selection
      className="mini"
      disabled={attributes.length === 0}
      options={options}
      value={selected}
      onChange={onChange}
      placeholder="Pick an attribute"
    />
);
};

/* eslint-enable no-param-reassign */
class Chart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { selectedAttribute: undefined };
    this.createChart();
  }

  createChart() {
    const xScale = new Plottable.Scales.Time()
      .domain([new Date(2000, 0, 1), new Date(2016, 11, 31)]);
    const yScale = new Plottable.Scales.Linear();
    const xAxis = new Plottable.Axes.Time(xScale, "bottom")
      .yAlignment("center");

    const yAxis = new Plottable.Axes.Numeric(yScale, "left");

    this.colorScale = new Plottable.Scales.Color();
    this.plot = new Plottable.Plots.Line()
      .x(d => new Date(d.timestamp), xScale)
      .y(d => d.value, yScale);
    this.plot.attr("stroke", (d, i, dataset) => dataset.metadata().name, this.colorScale);
    this.plot.autorangeMode("x");

    const legend = new Plottable.Components.Legend(this.colorScale).xAlignment("left")
      .maxEntriesPerRow(3);
    // const gridlines = new Plottable.Components.Gridlines(xScale, yScale);
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

  onAttributeChange({ value }) {
    this.setState({ selectedAttribute: value });
  }

  render() {
    const attributes = this.getAttributes(this.props.timeSeries);

    if (this.chart) {
      this.chart.redraw();
    }
    return (
      <div style={{ "visibility": this.props.visible ? "visible" : "hidden" }} className={styles.chart}>
        <div className="pt-dark pt-card pt-elevation-3" style={{ height: "100%" }}>
          <h5>
            <Button
              iconName="cross"
              className={Classes.MINIMAL}
              onClick={() => this.props.onUiToggle("chart")}
            />
            Chart
          </h5>
          <div className={styles.plotArea}>
            <svg id="example" />
          </div>
        </div>
      </div>
    );
  }
}
export default SizeMe({ monitorHeight: true })(Chart);  // eslint-disable-line new-cap


// <div className={classNames("ui", "card", "fluid", styles.card)}>
//   <div className={classNames("ui", "content", styles.content)}>
//     <div className="header">
//       <a>
//         <i className="icon close" onClick={() => this.props.onUiToggle("chart")}></i>
//       </a>
//       <div className="right floated">
//         <AttributePicker
//           selected={this.state.selectedAttribute}
//           attributes={attributes}
//           onChange={(e, v) => this.onAttributeChange(v)}
//         />
//       </div>
//     </div>
//     <div className={styles.plotArea}>
//       <svg id="example" />
//     </div>
//   </div>
// </div>
