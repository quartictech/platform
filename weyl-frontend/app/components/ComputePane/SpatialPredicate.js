import React from "react";
import {
  Button,
  Classes,
  Intent,
} from "@blueprintjs/core";
import * as _ from "underscore";
import Select from "../Select";
import PredictingPicker from "../PredictingPicker";


class SpatialPredicate extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      selectedLayerA: null,
      selectedLayerB: null,
      selectedOperation: "within",
    };
    this.onComputeClick = this.onComputeClick.bind(this);
    this.onLayerAChange = this.onLayerAChange.bind(this);
    this.onLayerBChange = this.onLayerBChange.bind(this);
    this.onOperationChange = this.onOperationChange.bind(this);
    this.isValid = this.isValid.bind(this);
    this.layerIdsToNames = this.layerIdsToNames.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    // Invalidate if layers are closed
    if (!nextProps.layers.has(this.state.selectedLayerA)) {
      this.onLayerAChange(null);
    }
    if (!nextProps.layers.has(this.state.selectedLayerB)) {
      this.onLayerBChange(null);
    }
  }

  render() {
    return (
      <div>
        <label className={Classes.LABEL}>
          <div>Layer A</div>
          <PredictingPicker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedLayerA}
            onChange={this.onLayerAChange}
          />
        </label>

        <label className={Classes.LABEL}>
          <div>Layer B</div>
          <PredictingPicker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedLayerB}
            onChange={this.onLayerBChange}
          />
        </label>

        <label className={Classes.LABEL} htmlFor="aggregation">
          Operation
          <div className="pt-control-group" id="aggregation">
            <Select
              entries={{
                equals: "Equals",
                disjoint: "Disjoint",
                touches: "Touches",
                contains: "Contains",
                covers: "Covers",
                intersects: "Intersects",
                within: "Within",
                covered_by: "Covered By",
                crosses: "Crosses",
                overlaps: "Overlaps",
              }}
              selectedKey={this.state.selectedOperation}
              onChange={this.onOperationChange}
            />
          </div>
        </label>

        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
          <Button
            iconName="calculator"
            text="Compute"
            disabled={!this.isValid()}
            intent={Intent.PRIMARY}
            loading={this.props.active}
            onClick={this.onComputeClick}
          />
        </div>
      </div>
    );
  }

  isValid() {
    return (
      (this.state.selectedLayerA) &&
      (this.state.selectedLayerB) &&
      (this.state.selectedOperation)
    );
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }

  onLayerAChange(value) {
    this.setState({ selectedLayerA: value });
  }

  onLayerBChange(value) {
    this.setState({ selectedLayerB: value });
  }

  onOperationChange(value) {
    this.setState({ selectedOperation: value });
  }

  onComputeClick() {
    const computeSpec = {
      type: "spatial_predicate",
      layerA: this.state.selectedLayerA,
      layerB: this.state.selectedLayerB,
      predicate: this.state.selectedOperation.toUpperCase(),
    };

    this.props.onComputationStart(computeSpec);
  }
}

export default SpatialPredicate;
