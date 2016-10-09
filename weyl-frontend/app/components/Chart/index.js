/**
*
* LineChart
*
*/

import React from "react";

import styles from "./styles.css";

import SizeMe from "react-sizeme";
const $ = require("jquery");

import LayerPicker from "../LayerPicker";

import * as Plottable from "plottable";
import "plottable/plottable.css";
import moment from "moment";
import classNames from "classnames";

import { Dropdown } from 'semantic-ui-react'

const AttributePicker = ({selected, attributes, onChange}) => {
  let options = attributes.map(attribute => ({text: attribute, value: attribute}));
  return (
  <Dropdown
    selection
    className="mini"
    disabled={attributes.length == 0}
    options={options}
    value={selected}
    onChange={onChange}
    placeholder='Pick an attribute'
  />
)}

/* eslint-enable no-param-reassign */
class Chart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { selectedAttribute: undefined };
    var xScale = new Plottable.Scales.Time()
    .domain([new Date(2000, 0, 1), new Date(2016, 11, 31)]);
    var yScale = new Plottable.Scales.Linear();

    var xAxis = new Plottable.Axes.Time(xScale, "bottom")
    .yAlignment("center");

    var yAxis = new Plottable.Axes.Numeric(yScale, "left");

    this.colorScale = new Plottable.Scales.Color();
    this.plot = new Plottable.Plots.Line()
      .x(function(d) { return new Date(d.timestamp); }, xScale)
      .y(function(d) { return d.value; }, yScale);
    this.plot.attr("stroke", function(d, i, dataset) { return dataset.metadata().name; }, this.colorScale);
    this.plot.autorangeMode("x");
    //this.plot.attr("fill", function(d, i, dataset) { return dataset.metadata().name; }, this.colorScale);

    let legend = new Plottable.Components.Legend(this.colorScale);
    this.chart = new Plottable.Components.Table([
      [legend, null],
      [yAxis, this.plot],
      [null, xAxis],
    ]);
  }

  componentDidMount() {
    this.chart.renderTo("svg#example");

    window.addEventListener("resize", function() {
      this.chart.redraw();
    });
  }

  getAttributes = (timeSeries) => timeSeries !== undefined ?  Object.keys(timeSeries) : [];

  updateChart(data) {
    this.colorScale.domain(Object.keys(data));
    let datasets = Object.keys(data)
      .map(k => {
          let plottableData = data[k].series.sort( (a, b) => a.timestamp - b.timestamp);
          return new Plottable.Dataset(plottableData, { "name": k });
      });
    this.plot.datasets(datasets);
  }

  componentWillReceiveProps(nextProps) {
    const attributes = this.getAttributes(nextProps.timeSeries);

    if (attributes.length == 0) {
      this.setState({ selectedAttribute: undefined });
    }
    else if (this.state.selectedAttribute === undefined) {
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

  onAttributeChange({ name, value }) {
    this.setState({ selectedAttribute: value });
  }

  render() {
    const attributes = this.getAttributes(this.props.timeSeries);

    if (this.chart) {
      this.chart.redraw();
    }
    return (
      <div style={{ "visibility": this.props.visible ? "visible" : "hidden" }} className={styles.chart}>
        <div className={classNames("ui", "card", "fluid", styles.card)}>
          <div className={classNames("ui", "content", styles.content)}>
            <div className="header">
              <a>
                <i className="icon close" onClick={() => this.props.onUiToggle("chart")}></i>
              </a>
              <div className="right floated">
                <AttributePicker
                  selected={this.state.selectedAttribute}
                  attributes={attributes}
                  onChange={ (e, v) => this.onAttributeChange(v) }
                />
              </div>
            </div>
            <div className={styles.plotArea}>
              <svg id="example" />
            </div>
          </div>
        </div>
      </div>
    );
  }
}
export default SizeMe({ monitorHeight: true })(Chart);  // eslint-disable-line new-cap
