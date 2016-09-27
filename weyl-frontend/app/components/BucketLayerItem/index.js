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
    this.onComputeClick = this.onComputeClick.bind(this);
    this.onAggregationChange = this.onAggregationChange.bind(this);
    this.onNormalizeToAreaChange = this.onNormalizeToAreaChange.bind(this);
    this.onBucketsChange = this.onBucketsChange.bind(this);
    this.onLayerChange = this.onLayerChange.bind(this);
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
    this.setState({ selectedLayer: value });
  }

  onBucketsChange(value) {
    this.setState({ selectedBuckets: value });
  }

  onCancelClick() {
    this.props.onUiToggle("bucket");
  }

  onNormalizeToAreaChange(e) {
    this.state.normalizeToArea = e.currentTarget.checked;
  }

  onAggregationChange(value) {
    this.setState({ selectedAggregation: value });
  }

  onComputeClick() {
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
    if (this.state.selectedAggregation === "sum" || this.state.selectedAggregation === "mean") {
      return (
        <div className="field">
          <LayerAttributePicker selected={this.state.selectedAttribute} attributes={numericAttributes} onChange={(v) => this.setState({ selectedAttribute: v })} />
        </div>);
    }
    return null;
  }

  render() {
    const selectedFeatureLayer = this.props.layers.find(layer => layer.id === this.state.selectedLayer);
    const numericAttributes = [];
    if (selectedFeatureLayer !== undefined) {
      for (const key of Object.keys(selectedFeatureLayer.attributeSchema.attributes)) {
        const attribute = selectedFeatureLayer.attributeSchema.attributes[key];
        if (attribute.type === "NUMERIC") {
          numericAttributes.push(key);
        }
      }
    }
    return (
      <div className={styles.bucketLayerItem}>
        <a onClick={() => this.onCancelClick()}>
          <i className="icon close"></i>
        </a><b>Bucket Layer</b>
        <div className="ui secondary segment">
          <div className="ui form">
            <div className="two fields">
              <div className="field">
                <LayerPicker layers={this.props.layers} label="Layer" onChange={this.onLayerChange} />
              </div>
              <div className="field">
                <LayerPicker layers={this.props.layers} label="Buckets" onChange={this.onBucketsChange} />
              </div>
            </div>

            <div className="field">
              <div className="ui checkbox">
                <input name="normalizeToArea" type="checkbox" onChange={this.onNormalizeToAreaChange} />
                <label htmlFor="normalizeToArea">Normalise to Area</label>
              </div>
            </div>

            <div className="two fields">
              <div className="field">
                <div className="ui fluid mini floating labeled icon dropdown button" ref={x => $(x).dropdown({ onChange: this.onAggregationChange })}>
                  <i className="calculator icon"></i>
                  <span className="text">Aggregate By</span>
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
            </div>
          </div>
          <div className="ui content">
            <button className="ui mini button primary" onClick={this.onComputeClick}>Compute</button>
          </div>
        </div>
      </div>
    );
  }
}

export default BucketLayerItem;
