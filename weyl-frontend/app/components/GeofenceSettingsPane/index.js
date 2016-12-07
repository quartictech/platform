import React from "react";
import {
  Button,
  Classes,
  Intent,
  Switch,
  Tag,
  Slider,
} from "@blueprintjs/core";
import * as _ from "underscore";
import Pane from "../Pane";
import PredictingPicker from "../PredictingPicker";

class GeofenceSettingsPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.layerIdsToNames = this.layerIdsToNames.bind(this);
  }

  render() {
    return (
      <Pane
        title="Geofence"
        iconName="polygon-filter"
        visible={this.props.visible}
        onClose={this.props.onClose}
        extraHeaderContent={
          <Tag
            className={Classes.MINIMAL}
            intent={Intent.SUCCESS}
          >
            {this.props.geofence.geojson.features.length} zones
          </Tag>
        }
      >
        <Switch
          label="Browser alerts"
          checked={this.props.geofence.alertsEnabled}
          onChange={this.props.onToggleAlerts}
        />

        <label className={Classes.LABEL}>
          <div>Target layer</div>
          <PredictingPicker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.props.geofence.layerId}
            onChange={layerId => this.props.onEdit.setLayer(layerId, 150)}
            disabled={!this.props.geofence.editing}
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
              onChange={v => this.props.onEdit.setBufferDistance(v)}
              value={this.props.geofence.bufferDistance}
            />
          </div>
        </label>

        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
          <Button
            iconName={this.props.geofence.editing ? "floppy-disk" : "edit"}
            text={this.props.geofence.editing ? "Save" : "Edit"}
            intent={Intent.PRIMARY}
            onClick={this.props.geofence.editing ? this.props.onEdit.finish : this.props.onEdit.start}
          />
        </div>
      </Pane>
    );
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }
}

export default GeofenceSettingsPane;
