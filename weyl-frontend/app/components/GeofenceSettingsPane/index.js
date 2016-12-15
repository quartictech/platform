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
    this.updateDisplayedSettings = this.updateDisplayedSettings.bind(this);
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
        extraHeaderContent={[
          tag(this.props.geofence.violations.numSevere, "", Intent.DANGER),
          tag(this.props.geofence.violations.numWarning, "", Intent.WARNING),
          tag(this.props.geofence.violations.numInfo, "", Intent.PRIMARY),
          tag(this.props.geofence.geojson.features.length, "zone(s)", Intent.SUCCESS),
        ]}
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
              onChange={mode => this.updateDisplayedSettings({ mode })}
            />

            <PredictingPicker
              iconName="layers"
              placeholder="Select layer..."
              entries={this.layerIdsToNames()}
              selectedKey={this.state.settings.layerId}
              onChange={layerId => this.updateDisplayedSettings({ layerId })}
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
          <div>Default alert level</div>
          <Select
            entries={{ INFO: "Info", WARNING: "Warning", SEVERE: "Severe" }}
            selectedKey={this.state.settings.defaultLevel}
            onChange={defaultLevel => this.updateDisplayedSettings({ defaultLevel })}
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
              onChange={bufferDistance => this.updateDisplayedSettings({ bufferDistance })}
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
      (a.defaultLevel !== b.defaultLevel) ||
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

const tag = (count, suffix, intent) =>
  (count > 0) && <Tag key={intent} className={Classes.MINIMAL} intent={intent}>{count} {suffix}</Tag>;

export default GeofenceSettingsPane;
