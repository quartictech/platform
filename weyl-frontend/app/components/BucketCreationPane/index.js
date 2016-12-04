import React from "react";
import {
  Button,
  Checkbox,
  Classes,
  Intent,
} from "@blueprintjs/core";
import * as _ from "underscore";
import Pane from "../Pane";
import Select from "../Select";
import PredictingPicker from "../PredictingPicker";

class BucketCreationPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      selectedLayer: null,
      selectedBuckets: null,
      selectedAggregation: "count",
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
    this.layerIdsToNames = this.layerIdsToNames.bind(this);
    this.attributeNames = this.attributeNames.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    // Invalidate if layers are closed
    if (!nextProps.layers.has(this.state.selectedLayer)) {
      this.onLayerChange(null);
    }
    if (!nextProps.layers.has(this.state.selectedBuckets)) {
      this.onBucketsChange(null);
    }
  }

  render() {
    return (
      <Pane
        title="Bucket"
        iconName="helper-management"
        visible={this.props.visible}
        onClose={this.props.onClose}
      >
        <label className={Classes.LABEL}>
          <div>Entities</div>
          <PredictingPicker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedLayer}
            onChange={this.onLayerChange}
          />
        </label>

        <label className={Classes.LABEL}>
          <div>Buckets</div>
          <PredictingPicker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedBuckets}
            onChange={this.onBucketsChange}
          />
        </label>

        <label className={Classes.LABEL} htmlFor="aggregation">
          Aggregation
          <div className="pt-control-group" id="aggregation">
            <Select
              entries={{ count: "Count", sum: "Sum of", mean: "Mean of" }}
              selectedKey={this.state.selectedAggregation}
              onChange={this.onAggregationChange}
            />

            <PredictingPicker
              iconName="property"
              placeholder="Select attribute..."
              entries={this.attributeNames()}
              disabled={!this.requiresAttribute(this.state.selectedAggregation)}
              selectedKey={this.state.selectedAttribute}
              onChange={(v) => this.setState({ selectedAttribute: v })}
            />
          </div>
        </label>

        <Checkbox
          label="Normalise to area"
          checked={this.state.normalizeToArea}
          onChange={this.onNormalizeToAreaChange}
        />

        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
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
      (this.state.selectedLayer) &&
      (this.state.selectedBuckets) &&
      (!this.requiresAttribute(this.state.selectedAggregation) || (this.state.selectedAttribute))
    );
  }

  requiresAttribute(aggregation) {
    return _.contains(["sum", "mean"], aggregation);
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }

  attributeNames() {
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
    return numericAttributes;
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
    this.setState({
      selectedAggregation: value,
      selectedAttribute: this.requiresAttribute(value) ? this.state.selectedAttribute : null,
    });
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

    if (this.requiresAttribute(this.state.selectedAggregation)) {
      computeSpec.aggregation.attribute = this.state.selectedAttribute;
    }

    this.props.onCompute(computeSpec);
  }
}

export default BucketCreationPane;
