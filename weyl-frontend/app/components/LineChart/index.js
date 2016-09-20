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

const linspace = function linspace(a, b, n) {
  if (typeof n === "undefined") n = Math.max(Math.round(b - a) + 1, 1);
  if (n < 2) { return n === 1 ? [a] : []; }
  const ret = Array(n);
  n--;
  for (let i = n; i >= 0; i--) { ret[i] = (i * b + (n - i) * a) / n; }
  return ret;
}

class LineChart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { hist: [] };
  }

  componentDidMount() {
    $(this.attributeDropdown).dropdown();
  }

  onLayerChange(value) {
    console.log("On layer change");
    this.props.onLayerSelection(value);
  }

  onAttributeChange(value) {
    this.props.onAttributeSelection(value);
  }

  componentWillReceiveProps(nextProps) {
    const attributes = Object.keys(nextProps.numericAttributes);
    if (attributes.length > 0) {
      let prop = nextProps.chart.selectedAttribute;
      if (prop == null || attributes.find(x => x === prop) === "undefined") {
        prop = attributes[0];
        this.onAttributeChange(prop);
      }
      const values = [];
      console.log(prop);

      for (let i = 0; i < nextProps.numericAttributes[prop].length ;i++) {
        const value = nextProps.numericAttributes[prop][i];
        if (!isNaN(value) && value != null) {
          values.push(value);
        }
      }
      const minValue = Math.min.apply(Math, values);
      const maxValue = Math.max.apply(Math, values);
      const x = linspace(minValue, maxValue, 10);
      this.state.hist = histogram({ data: values, bins: x }).map(bin => ({
        x: bin.x,
        y: bin.y,
      }));
    }
  }

  render() {
    const attributes = Object.keys(this.props.numericAttributes);
    const chartSeries = [{
      field: "y",
      name: this.props.chart.selectedAttribute,
      color: "#ff7f0e",
    }];

    return (
      <div style={{ "visibility": this.props.visible ? "visible" : "hidden" }} className={styles.lineChart}>
        <div className="ui card fluid">
          <div className="ui content" style={{ "padding": "5px" }}>
            <div className="ui grid">
              <div className="four wide column">
                <LayerPicker layers={this.props.layers} label="Pick Layer" onChange={this.onLayerChange.bind(this)} />
                <br />
                <LayerAttributePicker attributes={attributes} label="Pick Attribute" onChange={this.onAttributeChange.bind(this)} selected={this.props.chart.selectedAttribute} />
              </div>
              <div className="ui vertical divider">
                ->
              </div>
              <div className="twelve wide column">
                <BarChart
                  showXGrid={false}
                  showYGrid={false}
                  title="test"
                  data={this.state.hist}
                  chartSeries={chartSeries}
                  x={p => p.x}
                  width={12 / 16 * this.props.size.width}
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

LineChart.PropTypes = {
  layers: React.PropTypes.array,
  onLayerSelection: React.PropTypes.func,
  onAttributeSelection: React.PropTypes.func,
  data: React.PropTypes.array,
};

export default SizeMe({ monitorHeight:true })(LineChart);
