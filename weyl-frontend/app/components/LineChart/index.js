/**
*
* LineChart
*
*/

import React from "react";

import styles from "./styles.css";

import { BarChart } from "react-d3-basic";
import SizeMe from "react-sizeme";
const $ = require("jquery");

import LayerPicker from "../LayerPicker";
import LayerAttributePicker from "../LayerAttributePicker";

import * as histogram from "histogramjs";
import * as d3 from "d3";

/* eslint-disable no-param-reassign */
const linspace = function linspace(a, b, n) {
  if (typeof n === "undefined") n = Math.max(Math.round(b - a) + 1, 1);
  if (n < 2) { return n === 1 ? [a] : []; }
  const ret = Array(n);
  n--;
  for (let i = n; i >= 0; i--) { ret[i] = ((i * b) + ((n - i) * a)) / n; }
  return ret;
};
/* eslint-enable no-param-reassign */

class LineChart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { selectedLayer: null, selectedAttribute: null };
    this.onLayerChange = this.onLayerChange.bind(this);
    this.this.onAttributeChange = this.onAttributeChange.bind(this);
  }

  componentDidMount() {
    $(this.attributeDropdown).dropdown();
  }

  onLayerChange(value) {
    this.setState({ selectedLayer: value });
    this.props.onLayerSelection(value);
  }

  onAttributeChange(value) {
    this.setState({ selectedAttribute: value });
  }

  componentWillReceiveProps(nextProps) {
    const attributes = Object.keys(nextProps.numericAttributes);
    if (attributes.length > 0 &&
        (this.state.selectedAttribute == null || !(this.state.selectedAttribute in nextProps.numericAttributes))) {
      this.setState({ selectedAttribute: attributes[0] });
    }
  }

  updateHist() {
    const attributes = Object.keys(this.props.numericAttributes);
    const selected = this.state.selectedAttribute;

    if (attributes.length > 0) {
      const values = [];

      for (let i = 0; i < this.props.numericAttributes[selected].length; i++) {
        const value = this.props.numericAttributes[selected][i];
        if (!isNaN(value) && value != null) {
          values.push(value);
        }
      }
      const minValue = Math.min(...values);
      const maxValue = Math.max(...values);
      const x = linspace(minValue, maxValue, 10);
      return histogram({ data: values, bins: x }).map(bin => ({
        x: bin.x,
        y: bin.y,
      }));
    }

    return [];
  }

  render() {
    const attributes = Object.keys(this.props.numericAttributes);
    const chartSeries = [{
      field: "y",
      name: this.state.selectedAttribute,
      color: "#ff7f0e",
    }];

    return (
      <div style={{ "visibility": this.props.visible ? "visible" : "hidden" }} className={styles.lineChart}>
        <div className="ui card fluid">
          <div className="ui content" style={{ "padding": "5px" }}>
            <div className="ui grid">
              <div className="four wide column">
                <LayerPicker layers={this.props.layers} label="Pick Layer" onChange={this.onLayerChange} />
                <br />
                <LayerAttributePicker attributes={attributes} label="Pick Attribute" onChange={this.onAttributeChange} selected={this.state.selectedAttribute} />
              </div>
              <div className="ui vertical divider">
                ->
              </div>
              <div className="twelve wide column">
                <BarChart
                  showXGrid={false}
                  showYGrid={false}
                  title="test"
                  data={this.updateHist()}
                  chartSeries={chartSeries}
                  x={p => p.x}
                  width={(12 / 16) * this.props.size.width}
                  height={this.props.size.height - 20}
                  xScale="ordinal"
                  xTickFormat={d3.format(".2s")}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default SizeMe({ monitorHeight: true })(LineChart);
