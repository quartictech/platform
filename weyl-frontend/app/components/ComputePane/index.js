import React from "react";
import {
  Position,
} from "@blueprintjs/core";
import Pane from "../Pane";
import NormalPicker from "../NormalPicker";
import Bucket from "./Bucket";
import Buffer from "./Buffer";
import SpatialPredicate from "./SpatialPredicate";

class ComputePane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      operation: "Bucket",
    };
    this.onOperationChange = this.onOperationChange.bind(this);
  }

  renderComputationSettings() {
    switch (this.state.operation) {
      case "Bucket":
        return (
          <Bucket
            layers={this.props.layers}
            active={this.props.computation.active}
            onComputationStart={this.props.onComputationStart}
          />);
      case "Buffer":
        return (
          <Buffer
            layers={this.props.layers}
            active={this.props.computation.active}
            onComputationStart={this.props.onComputationStart}
          />);
      case "Spatial Predicate":
        return (
          <SpatialPredicate
            layers={this.props.layers}
            active={this.props.computation.active}
            onComputationStart={this.props.onComputationStart}
          />);
      default:
        throw Error(`unrecognised operation:  + ${this.state.operation}`);
    }
  }

  render() {
    return (
      <Pane
        title="Compute"
        iconName="calculator"
        visible={this.props.visible}
        onClose={this.props.onClose}
        extraHeaderContent={
          <NormalPicker
            iconName="function"
            position={Position.TOP}
            selected={this.state.operation}
            entries={["Bucket", "Buffer", "Spatial Predicate"]}
            onChange={this.onOperationChange}
          />
        }
      >
        { this.renderComputationSettings() }
      </Pane>
    );
  }

  onOperationChange(operation) {
    this.setState({ operation });
  }
}

export default ComputePane;
