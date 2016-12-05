import React from "react";
import {
  Button,
  Classes,
  InputGroup,
  Intent,
  Popover,
  Position,
} from "@blueprintjs/core";
import classNames from "classnames";
import * as _ from "underscore";
import * as $ from "jquery";
import Select from "../Select";
import PredictingPicker from "../PredictingPicker";


class Buffer extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      selectedLayer: null,
      distance: "",
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
          <div>Distance (m)</div>
          <InputGroup
            placeholder="Specify distance..."
            value={this.state.distance}
            onChange={(e) => this.setState({ distance: e.target.value })}
            intent={this.isValidDistance() ? Intent.NONE : Intent.DANGER}
          />
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
