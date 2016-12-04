import React from "react";
import {
  Button,
  Checkbox,
  Intent,
} from "@blueprintjs/core";
import * as _ from "underscore";
import Pane from "../Pane";
import Select from "../Select";

class BucketCreationPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      selectedLayer: null,
      selectedBuckets: null,
      selectedAggregation: null,
      selectedAttribute: null,
      normalizeToArea: false,
    };
    this.onComputeClick = this.onComputeClick.bind(this);
    this.onAggregationChange = this.onAggregationChange.bind(this);
    this.onNormalizeToAreaChange = this.onNormalizeToAreaChange.bind(this);
    this.onBucketsChange = this.onBucketsChange.bind(this);
    this.onLayerChange = this.onLayerChange.bind(this);
    this.isValid = this.isValid.bind(this);
    this.requiresAttribute = this.requiresAttribute.bind(this);
  }

  onComputeClick() {
    const computeSpec = {
      type: "bucket",
      features: this.state.selectedLayer,
      buckets: this.state.selectedBuckets,
      aggregation: {
        type: this.state.selectedAggregation,
      },
      normalizeToArea: this.state.normalizeToArea,
    };

    if (this.state.selectedAggregation === "sum" || this.state.selectedAggregation === "mean") {
      computeSpec.aggregation.attribute = this.state.selectedAttribute;
    }

    this.props.onCompute(computeSpec);
  }

  render() {
    const selectedFeatureLayer = this.props.layers.get(this.state.selectedLayer);
    const numericAttributes = [];
    if (selectedFeatureLayer !== undefined) {
      for (const key of Object.keys(selectedFeatureLayer.toJS().attributeSchema.attributes)) {
        const attribute = selectedFeatureLayer.toJS().attributeSchema.attributes[key];
        if (attribute.type === "NUMERIC") {
          numericAttributes.push(key);
        }
      }
    }
    return (
      <Pane
        title="Bucket"
        iconName="helper-management"
        visible={this.props.visible}
        onClose={this.props.onClose}
      >

        <Select
          label="Entities"
          default="Select entities layer..."
          entries={_.object(_.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])]))}
          selectedKey={this.state.selectedLayer}
          onChange={this.onLayerChange}
        />

        <Select
          label="Buckets"
          default="Select buckets layer..."
          entries={_.object(_.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])]))}
          selectedKey={this.state.selectedBuckets}
          onChange={this.onBucketsChange}
        />

        <Checkbox
          label="Normalise to area"
          checked={this.state.normalizeToArea}
          onChange={this.onNormalizeToAreaChange}
        />

        <Select
          label="Aggregate type"
          default="Select aggregation..."
          entries={{ count: "Count", sum: "Sum", mean: "Mean" }}
          selectedKey={this.state.selectedAggregation}
          onChange={this.onAggregationChange}
        />

        <div style={{ display: this.requiresAttribute() ? "block" : "none" }}>
          <Select
            label="Aggregate by"
            default="Select attribute..."
            entries={numericAttributes}
            selectedKey={this.state.selectedAttribute}
            onChange={(v) => this.setState({ selectedAttribute: v })}
          />
        </div>

        <div style={{ textAlign: "right" }}>
          <Button
            iconName="calculator"
            text="Compute"
            disabled={!this.isValid()}
            intent={Intent.PRIMARY}
            onClick={this.onComputeClick}
          />
        </div>
      </Pane>
    );
  }

  isValid() {
    return (
      (this.state.selectedLayer !== null) &&
      (this.state.selectedBuckets !== null) &&
      (this.state.selectedAggregation !== null) &&
      (!this.requiresAttribute() || (this.state.selectedAttribute !== null))
    );
  }

  requiresAttribute() {
    return _.contains(["sum", "mean"], this.state.selectedAggregation);
  }

  onLayerChange(value) {
    // Avoid unnecessarily resetting attribute
    if (value !== this.state.selectedLayer) {
      this.setState({ selectedLayer: value, selectedAttribute: null });
    }
  }

  onBucketsChange(value) {
    this.setState({ selectedBuckets: value });
  }

  onNormalizeToAreaChange(e) {
    this.setState({ normalizeToArea: e.currentTarget.checked });
  }

  onAggregationChange(value) {
    this.setState({ selectedAggregation: value });
  }
}

export default BucketCreationPane;
