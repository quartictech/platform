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
import Select from "../Select";
import PredictingPicker from "../PredictingPicker";

class GeofenceSettingsPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.layerIdsToNames = this.layerIdsToNames.bind(this);
    this.state = {
      displayedSettings: this.props.geofence.settings,
    };
    this.onModeChange = this.onModeChange.bind(this);
    this.onLayerChange = this.onLayerChange.bind(this);
    this.onBufferDistanceChange = this.onBufferDistanceChange.bind(this);
    this.commitChanges = this.commitChanges.bind(this);
    this.undoChanges = this.undoChanges.bind(this);
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
          <div>Geometry</div>
          <div className="pt-control-group" id="aggregation">
            <Select
              entries={{ layer: "From layer", manual: "Manual" }}
              selectedKey={this.state.displayedSettings.mode}
              onChange={this.onModeChange}
            />

            <PredictingPicker
              iconName="layers"
              placeholder="Select layer..."
              entries={this.layerIdsToNames()}
              selectedKey={this.state.displayedSettings.layerId}
              onChange={this.onLayerChange}
              disabled={this.state.displayedSettings.mode !== "layer"}
            />
          </div>
        </label>


        <label className={Classes.LABEL}>
          <div>Buffer distance (m)</div>
          <div style={{ margin: "10px" }}>
            <Slider
              min={0}
              max={1000}
              stepSize={10}
              labelStepSize={200}
              value={this.state.displayedSettings.bufferDistance}
              onChange={this.onBufferDistanceChange}
            />
          </div>
        </label>

        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
          <Button
            iconName="undo"
            text="Undo"
            onClick={this.undoChanges}
            disabled={!this.uncommittedChanges()}
          />
          <Button
            iconName="floppy-disk"
            text="Save"
            intent={Intent.PRIMARY}
            onClick={this.commitChanges}
            disabled={!this.uncommittedChanges() || !this.isValid()}
          />
        </div>
      </Pane>
    );
  }

  uncommittedChanges() {
    console.log(this.props.geofence);
    const a = this.state.displayedSettings;
    const b = this.props.geofence.settings;
    return (
      (a.mode !== b.mode) ||
      (a.layerId !== b.layerId) ||
      (a.bufferDistance !== b.bufferDistance)
    );
  }

  isValid() {
    return (this.state.displayedSettings.mode === "manual") || (this.state.displayedSettings.layerId);
  }

  commitChanges() {
    console.log("displayedSettings", this.state.displayedSettings);
    this.props.onCommitSettings(this.state.displayedSettings);
  }

  undoChanges() {
    this.updateDisplayedSettings(this.props.geofence.settings);
  }

  onModeChange(mode) {
    this.updateDisplayedSettings({ mode });
  }

  onLayerChange(layerId) {
    this.updateDisplayedSettings({ layerId });
  }

  onBufferDistanceChange(bufferDistance) {
    this.updateDisplayedSettings({ bufferDistance });
  }

  updateDisplayedSettings(newSettings) {
    const displayedSettings = { ...this.state.displayedSettings, ...newSettings };
    this.props.onSetManualControlsVisibility(displayedSettings.mode === "manual");
    this.setState({ displayedSettings });
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }
}

const initialSettings = () => ({
  layerId: null,
  bufferDistance: 250,
  mode: "layer",
});


export default GeofenceSettingsPane;
