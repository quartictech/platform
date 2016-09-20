/**
*
* BucketLayerItem
*
*/

import React from "react";

import styles from "./styles.css";
const $ = require("jquery");

import LayerPicker from "../LayerPicker";
import LayerAttributePicker from "../LayerAttributePicker";

class BucketLayerItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = { selectedLayer: null, selectedBuckets: null, normalizeToArea: false };
  }

  componentDidMount() {
    $("#aggregate-dropdown")
    .dropdown({
      onChange: (value) => {
        this.onAggregationChange(value);
      },
    });
  }

  onLayerChange(value) {
    this.state.selectedLayer = value;
    this.forceUpdate();
  }

  onBucketsChange(value) {
    this.state.selectedBuckets = value;
    this.forceUpdate();
  }

  onCancelClick() {
    this.props.onUiToggle("bucket");
  }

  onNormalizeToAreaChange(e) {
    this.state.normalizeToArea = e.currentTarget.checked;
    console.log(this.state.normalizeToArea);
  }

  onAggregationChange(value) {
    this.state.selectedAggregation = value;
    this.forceUpdate();
  }

  onComputeClick() {
    console.log("compute click");
    const computeSpec = {
      features: this.state.selectedLayer,
      buckets: this.state.selectedBuckets,
      aggregation: {
        type: this.state.selectedAggregation,
      },
      normalizeToArea: this.state.normalizeToArea,
    };

    if (this.state.selectedAggregation === "sum" || this.state.selectedAggregation === "mean") {
      computeSpec.aggregation.property = this.state.selectedAttribute;
    }

    this.props.onCompute(computeSpec);
  }

  renderAttributePicker(numericAttributes) {
    console.log(this.state.selectedAggregation);
    if (this.state.selectedAggregation === "sum" || this.state.selectedAggregation === "mean") {
      return (
        <div className="field">
          <LayerAttributePicker attributes={numericAttributes} onChange={(v) => this.state.selectedAttribute = v} />
        </div>);
    }
    return null;
  }

  render() {
    const selectedFeatureLayer = this.props.layers.find(layer => layer.id === this.state.selectedLayer);
    const numericAttributes = [];
    if (selectedFeatureLayer != null) {
      for (const key of Object.keys(selectedFeatureLayer.attributeSchema.attributes)) {
        const attribute = selectedFeatureLayer.attributeSchema.attributes[key];
        if (attribute.type === "NUMERIC") {
          numericAttributes.push(key);
        }
      }
    }
    return (
      <div className={styles.bucketLayerItem}>
        <div className="ui raised fluid card">
          <div className="content">
            <div className="header">Bucket Layer</div>
            <div className="meta">Take geographical features from one layer and assign them to polygons of another, aggregating the results.</div>
          </div>
          <div className="content">
            <form className="ui form">
              <div className="field">
                <LayerPicker layers={this.props.layers} label="Pick Layer" onChange={this.onLayerChange.bind(this)} />
              </div>
              <div className="field">
                <LayerPicker layers={this.props.layers} label="Pick Buckets" onChange={this.onBucketsChange.bind(this)} />
              </div>

              <div className="field">
                <div id="aggregate-dropdown" className="ui floating labeled icon dropdown button">
                  <i className="filter icon"></i>
                  <span className="text">Aggregate</span>
                  <div className="menu">
                    <div className="item" data-value="count">
                      Count
                    </div>
                    <div className="item" data-value="sum">
                      Sum
                    </div>
                    <div className="item" data-value="mean">
                      Average
                    </div>
                  </div>
                </div>
              </div>
              {this.renderAttributePicker(numericAttributes)}

              <div className="ui checkbox">
                <input name="normalizeToArea" type="checkbox" onChange={this.onNormalizeToAreaChange.bind(this)} />
                <label>Normalise to Area</label>
              </div>
            </form>
          </div>
          <div className="ui content">
            <button className="ui button primary" onClick={this.onComputeClick.bind(this)}>Compute</button>
            <button className="ui button red" onClick={this.onCancelClick.bind(this)}>Cancel</button>
          </div>
        </div>
      </div>
    );
  }
}

export default BucketLayerItem;
