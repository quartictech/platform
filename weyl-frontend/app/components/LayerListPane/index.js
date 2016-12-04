import React from "react";

import GeofenceSettings from "../GeofenceSettings";
import LayerListItem from "./LayerListItem";
import Pane from "../Pane";
const _ = require("underscore");


class LayerListPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      activeLayerId: null,
      activeMode: null,
    };
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

  render() {
    const rows = [];
    if (this.props.ui.layerOp === "geofence") {
      rows.push(<GeofenceSettings
        key="geofence"
        layers={this.props.layers}
        geofence={this.props.geofence}
        onGeofenceEdit={this.props.onGeofenceEdit}
        onToggleAlerts={this.props.onGeofenceToggleAlerts}
        onClose={() => this.props.onUiToggle("geofence")}
      />);
    }

    this.props.layers.toArray().forEach(layer =>
      rows.push(
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
    );

    if (_.size(this.props.layers.toArray()) > 0 || this.props.ui.layerOp != null) {
      return (
        <Pane
          title="Layers"
          iconName="layers"
          visible={this.props.visible}
          onClose={this.props.onClose}
        >
          <div className="ui divided items">
            {rows}
          </div>
        </Pane>
      );
    }
    return null;
  }
}

export default LayerListPane;
