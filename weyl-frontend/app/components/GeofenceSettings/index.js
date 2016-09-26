/**
*
* GeofenceSettings
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

const GeofenceButton = ({ editing, onEditClick, onSaveClick }) => {
  if (editing) {
    return (
      <button className="ui icon button primary" onClick={() => onSaveClick() }>
        <i className="save icon"></i>
      </button>);
  }
  else {
    return (
      <button className="ui icon button primary" onClick={() => onEditClick() }>
        <i className="pencil icon"></i>
      </button> );
  }
}

const GeofenceInfo = ({ geojson }) => {
  if (geojson != null) {
    return (
      <div className="ui label">Geofence Areas
        <div className="detail">{geojson.features.length}</div>
      </div>);
  }
  else {
    return null;
  }
}

class GeofenceSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
  }

  render() {
    return (
      <div className={styles.geofenceSettings}>
        <a onClick={() => this.onCancelClick()}>
          <i className="icon close"></i>
        </a><b>Geofence</b>
        <div className="ui secondary segment">
          <GeofenceButton editing={this.props.geofence.editing}
            onEditClick={this.props.onGeofenceEdit}
            onSaveClick={this.props.onGeofenceSave} />

          <GeofenceInfo geojson={this.props.geofence.geojson} />
        </div>
      </div>
    );
  }
}

export default GeofenceSettings;
