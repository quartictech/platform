import React from "react";

import BucketLayerItem from "../BucketLayerItem";
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

  render() {
    const rows = [];
    if (this.props.ui.layerOp === "bucket") {
      rows.push(<BucketLayerItem onCompute={this.props.onBucketCompute} layers={this.props.layers} onUiToggle={this.props.onUiToggle} key="bucket" />);
    } else if (this.props.ui.layerOp === "geofence") {
      rows.push(<GeofenceSettings
        key="geofence"
        geofence={this.props.geofence}
        onGeofenceEdit={this.props.onGeofenceEdit}
        onGeofenceSave={this.props.onGeofenceSave}
        onGeofenceChangeType={this.props.onGeofenceChangeType}
      />);
    }

    this.props.layers.toArray().forEach(layer =>
      rows.push(
        <LayerListItem
          key={layer.get("id")}
          layer={layer.toJS()}
          onButtonClick={(name) => this.onButtonClick(name, layer.get("id"))}
          onToggleValueVisible={this.props.onToggleValueVisible}
          onLayerStyleChange={this.props.onLayerStyleChange}
          mode={(this.state.activeLayerId === layer.get("id")) ? this.state.activeMode : null}
        />
      )
    );

    if (_.size(this.props.layers.toArray()) > 0 || this.props.ui.layerOp != null) {
      return (
        <Pane visible={this.props.visible}>
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
