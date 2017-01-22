import React from "react";
import {
  Position,
} from "@blueprintjs/core";
import Pane from "../Pane";
import NormalPicker from "../NormalPicker";
import Bucket from "./Bucket";
import Buffer from "./Buffer";

class ComputePane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      operation: "Bucket",
    };
    this.onOperationChange = this.onOperationChange.bind(this);
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
            entries={["Bucket", "Buffer"]}
            onChange={this.onOperationChange}
          />
        }
      >
        {
          (this.state.operation === "Bucket")
            ? (
            <Bucket
              layers={this.props.layers}
              active={this.props.computation.active}
              onComputationStart={this.props.onComputationStart}
            />
            )
            : (
            <Buffer
              layers={this.props.layers}
              active={this.props.computation.active}
              onComputationStart={this.props.onComputationStart}
            />
            )
          }
      </Pane>
    );
  }

  onOperationChange(operation) {
    this.setState({ operation });
  }
}

export default ComputePane;
