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

const AttributePicker = (value, attributes) => (
  <Dropdown
    selection
    compact
    className="mini"
    options={[{"text": "Hello", "value":0}]}
    placeholder='Pick an attribute'
  />
)

/* eslint-enable no-param-reassign */
class Chart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.chart = null;
  }

  componentDidMount() {
    var xScale = new Plottable.Scales.Time()
      .domain([new Date(2015, 0, 1), new Date(2016, 11, 31)]);
    var yScale = new Plottable.Scales.Linear();
    var data = [
      { x: "1/1/2015 9:00:00", y: 1 },
      { x: "4/1/2016 9:00:00", y: 3 },
      { x: "6/1/2016 9:00:00", y: 17 },
    ];

    var xAxis = new Plottable.Axes.Time(xScale, "bottom")
    .yAlignment("center");

    var yAxis = new Plottable.Axes.Numeric(yScale, "left");

    var plot = new Plottable.Plots.Area()
      .addDataset(new Plottable.Dataset(data))
      .x(function(d) { return new Date(d.x); }, xScale)
      .y(function(d) { return d.y; }, yScale);

      this.chart = new Plottable.Components.Table([
        [yAxis, plot],
        [null, xAxis]
      ])
      .renderTo("svg#example");

      window.addEventListener("resize", function() {
        this.chart.redraw();
      });
    }

  render() {
    if (this.chart) {
      this.chart.redraw();
    }
    return (
      <div style={{ "visibility": this.props.visible ? "visible" : "hidden" }} className={styles.chart}>
        <div className={classNames("ui", "card", "fluid", styles.card)}>
          <div className={classNames("ui", "content", styles.content)}>
            <div className="header">
              <a>
                <i className="icon close" onClick={() => onUiToggle("chart")}></i>
              </a>
              Time Series
              <div className="right floated">
                <AttributePicker/>
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
