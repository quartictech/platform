/**
*
* GeofenceSettings
*
*/

import React from "react";

import styles from "./styles.css";
const $ = require("jquery");
import { Dropdown } from "semantic-ui-react";

const GeofenceLayerItem = ({ layer, onSelect }) =>
(
  <Dropdown.Item
    text={layer.metadata.name}
    value={layer.id}
    onClick={(e, v) => onSelect(v)}
  />
);

const GeofenceLayerPicker = ({ visible, layers, onSelect }) => {
  if (!visible) {
    return null;
  }
  return (
    <Dropdown text="From Layer" button>
      <Dropdown.Menu>
        {layers.toArray().map(layer =>
          <GeofenceLayerItem
            key={layer.get("id")}
            layer={layer.toJS()}
            onSelect={onSelect}
          />)}
      </Dropdown.Menu>
    </Dropdown>
  );
};

const GeofenceTypeDropdown = ({ type, onTypeChange, visible }) => {
  if (visible) {
    return (
      <div className="inline field">
        <select className="ui fluid dropdown" ref={x => $(x).dropdown()} onChange={e => onTypeChange(e.currentTarget.value)} value={type}>
          <option value="EXCLUDE">Exclude</option>
          <option value="INCLUDE">Include</option>
        </select>
      </div>
    );
  }
  return null;
};

const GeofenceButton = ({ editing, onEditClick, onSaveClick }) => {
  const callback = (editing) ? onSaveClick : onEditClick;
  const icon = (editing) ? "save" : "pencil";
  return (
    <button className="ui icon button primary" onClick={() => callback()}>
      <i className={`${icon} icon`}></i>
    </button>
  );
};

const GeofenceInfo = ({ geojson }) => {
  if (geojson != null) {
    return (
      <div className="ui mini horizontal statistic">
        <div className="value">{geojson.features.length}</div>
        <div className="label">Zones</div>
      </div>);
  }
  return null;
};

class GeofenceSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div className={styles.geofenceSettings}>
        <a onClick={() => this.onCancelClick()}>
          <i className="icon close"></i>
        </a><b>Geofence</b>
        <div className="ui secondary segment">
          <div className="ui form">
            <div className="inline field">
              <GeofenceButton
                editing={this.props.geofence.editing}
                onEditClick={this.props.onGeofenceEdit}
                onSaveClick={() => this.props.onGeofenceSave(this.props.geofence)}
              />
              <GeofenceLayerPicker
                visible={this.props.geofence.editing}
                layers={this.props.layers}
                onSelect={layerId =>
                  this.props.onGeofenceSave({
                    type: this.props.geofence.type,
                    layerId,
                    bufferDistance: 10,
                  })}
              />
            </div>
            <GeofenceTypeDropdown visible={this.props.geofence.editing} type={this.props.geofence.type} onTypeChange={this.props.onGeofenceChangeType} key="dropdown" />
          </div>
          <GeofenceInfo geojson={this.props.geofence.geojson} />
        </div>
      </div>
    );
  }
}

export default GeofenceSettings;
