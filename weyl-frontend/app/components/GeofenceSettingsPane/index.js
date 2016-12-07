import React from "react";
import {
  Button,
  Classes,
  Intent,
  Switch,
  Tag,
  Slider,
} from "@blueprintjs/core";
import classNames from "classnames";
import * as _ from "underscore";
import Pane from "../Pane";
import Select from "../Select";
import PredictingPicker from "../PredictingPicker";

class GeofenceSettingsPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.layerIdsToNames = this.layerIdsToNames.bind(this);
    this.state = {
      settings: this.props.geofence.settings,
    };
    this.onModeChange = this.onModeChange.bind(this);
    this.onLayerChange = this.onLayerChange.bind(this);
    this.onBufferDistanceChange = this.onBufferDistanceChange.bind(this);
    this.commitChanges = this.commitChanges.bind(this);
    this.undoChanges = this.undoChanges.bind(this);
    this.beginManualEditing = this.beginManualEditing.bind(this);
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
              selectedKey={this.state.settings.mode}
              onChange={this.onModeChange}
            />

            <PredictingPicker
              iconName="layers"
              placeholder="Select layer..."
              entries={this.layerIdsToNames()}
              selectedKey={this.state.settings.layerId}
              onChange={this.onLayerChange}
              disabled={this.state.settings.mode !== "layer"}
            />
          </div>
        </label>

        <label className={classNames(Classes.LABEL, Classes.INLINE)}>
          Draw manually
          <Button
            iconName="edit"
            intent={Intent.PRIMARY}
            onClick={this.beginManualEditing}
            disabled={this.state.settings.mode !== "manual" || this.props.geofence.manualControlsVisible}
          />
        </label>

        <label className={Classes.LABEL}>
          <div>Buffer distance (m)</div>
          <div style={{ margin: "10px" }}>
            <Slider
              min={0}
              max={1000}
              stepSize={10}
              labelStepSize={200}
              value={this.state.settings.bufferDistance}
              onChange={(v) => this.onBufferDistanceChange(v)}
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
    const a = this.state.settings;
    const b = this.props.geofence.settings;
    return (
      (a.mode !== b.mode) ||
      (a.layerId !== b.layerId) ||
      (a.bufferDistance !== b.bufferDistance) ||
      this.props.geofence.manualControlsVisible
    );
  }

  isValid() {
    return (this.state.settings.mode === "manual") || (this.state.settings.layerId);
  }

  commitChanges() {
    this.props.onSetManualControlsVisibility(false);
    this.props.onCommitSettings(this.state.settings);
  }

  undoChanges() {
    this.props.onSetManualControlsVisibility(false);
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

  beginManualEditing() {
    this.props.onSetManualControlsVisibility(true);
  }

  updateDisplayedSettings(newSettings) {
    const settings = { ...this.state.settings, ...newSettings };
    if (settings.mode === "layer") {
      this.props.onSetManualControlsVisibility(false);
    }
    this.setState({ settings });
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }
}

export default GeofenceSettingsPane;
