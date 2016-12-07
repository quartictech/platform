import React from "react";
import {
  Button,
  Classes,
  InputGroup,
  Slider,
  Intent,
} from "@blueprintjs/core";
import * as _ from "underscore";
import * as $ from "jquery";
import PredictingPicker from "../PredictingPicker";


class Buffer extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      selectedLayer: null,
      distance: 50,
    };
    this.onLayerChange = this.onLayerChange.bind(this);
    this.onComputeClick = this.onComputeClick.bind(this);
  }

  render() {
    return (
      <div>
        <label className={Classes.LABEL}>
          <div>Base</div>
          <PredictingPicker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedLayer}
            onChange={this.onLayerChange}
          />
        </label>

        <label className={Classes.LABEL}>
          <div>Buffer Distance (m)</div>
          <div style={{ margin: "10px" }}>
            <Slider
              min={0}
              max={1000}
              stepSize={10}
              labelStepSize={200}
              onChange={v => this.setState({ distance: v })}
              value={this.state.distance}
            />
          </div>
        </label>

        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
          <Button
            iconName="calculator"
            text="Compute"
            disabled={!this.isValid()}
            intent={Intent.PRIMARY}
            onClick={this.onComputeClick}
          />
        </div>
      </div>
    );
  }

  isValid() {
    return (
      (this.state.selectedLayer) &&
      this.isValidDistance()
    );
  }

  isValidDistance() {
    return $.isNumeric(this.state.distance);
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }

  onLayerChange(value) {
    this.setState({ selectedLayer: value });
  }

  onComputeClick() {
    this.props.onCompute({
      type: "buffer",
      layerId: this.state.selectedLayer,
      bufferDistance: this.state.distance,
    });
  }
}

export default Buffer;
