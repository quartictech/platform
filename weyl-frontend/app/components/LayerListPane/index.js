import React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as _ from "underscore";
import LayerListItem from "./LayerListItem";
import Pane from "../Pane";


class LayerListPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      activeLayerId: null,
      activeMode: null,
    };
  }

  render() {
    return (
      <Pane
        title="Layers"
        iconName="layers"
        visible={this.props.visible && _.size(this.props.layers.toArray()) > 0}
        onClose={this.props.onClose}
      >
        <table className="pt-table pt-striped pt-condensed" style={{ width: "100%" }}>
          <tbody>
            {
              _.map(this.props.layers.toArray(), layer =>
                <LayerListItem
                  key={layer.get("id")}
                  layer={layer.toJS()}
                  onButtonClick={(name) => this.onButtonClick(name, layer.get("id"))}
                  onToggleValueVisible={this.props.onToggleValueVisible}
                  onLayerStyleChange={(attribute) => this.props.onLayerStyleChange(layer.get("id"), "ATTRIBUTE", attribute)}
                  onLayerThemeChange={(idx) => this.props.onLayerStyleChange(layer.get("id"), "THEME", idx)}
                  onBufferClick={(bufferDistance) => this.onBufferCompute(layer.get("id"), bufferDistance)}
                  mode={(this.state.activeLayerId === layer.get("id")) ? this.state.activeMode : null}
                />
              )
            }
          </tbody>
        </table>
      </Pane>
    );
  }

  onButtonClick(name, layerId) {
    switch (name) {
      case "VISIBLE":
        return this.props.layerToggleVisible(layerId);
      case "CLOSE":
        return this.props.layerClose(layerId);
      default:
        return this.setState({
          activeLayerId: layerId,
          activeMode: (this.state.activeLayerId === layerId && this.state.activeMode === name) ? null : name,
        });
    }
  }

  onBufferCompute(layerId, bufferDistance) {
    this.props.onCompute({
      type: "buffer",
      layerId,
      bufferDistance,
    });
  }
}

export default LayerListPane;
