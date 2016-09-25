/**
*
* GeofenceSettings
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

const GeofenceList = ({ items }) =>
  (<div className="ui relaxed divided list">
    <div className="item">
      <i className="large github middle aligned icon"></i>
      <div className="content">
        <a className="header">Geofence 1</a>
        <div className="description">Updated 10 mins ago</div>
      </div>
    </div>
    <div className="item">
      <i className="large github middle aligned icon"></i>
      <div className="content">
        <a className="header">Geofence 2</a>
        <div className="description">Updated 22 mins ago</div>
      </div>
    </div>
  </div>
);

class GeofenceSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div className={styles.geofenceSettings}>
        <a onClick={() => this.onCancelClick()}>
          <i className="icon close"></i>
        </a><b>Geofence</b>
        <div className="right floated">
          <button className="ui mini button primary">New</button>
        </div>
        <div className="ui secondary segment">
          <GeofenceList items={[]}/>
        </div>
      </div>
    );
  }
}

export default GeofenceSettings;
