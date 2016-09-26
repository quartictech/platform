/**
*
* GeofenceSettings
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

const GeofenceCreate = ({ visible }) => {
    if (visible) {
      return (
        <div className={styles.geofenceCreate}>
          <div className="ui form">
            <div className="ui field">
              <div className="ui fluid action input">
                <input placeholder="Geofence Name..." type="text"/>
                <button className="ui button mini icon"><i className="pencil icon"></i></button>
                <button className="ui mini icon button"><i className="save icon"></i></button>
              </div>
            </div>
          </div>
        </div>
      );
    }
    else {
      return null;
    }
}

const GeofenceList = ({ items }) =>
  (<div className="ui relaxed divided list">
    <div className="item">
      <i className="large right floated middle aligned icon close"></i>
      <div className="content">
        <a className="header">Geofence 1</a>
        <div className="description">Updated 10 mins ago</div>
      </div>
    </div>
    <div className="item">
      <i className="large right floated middle aligned icon close"></i>
      <div className="content">
        <a className="header">Geofence 2</a>
        <div className="description">Updated 22 mins ago</div>
      </div>
    </div>
  </div>
);

class GeofenceSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      creating: false,
    };
  }

  onNewClick() {
    this.setState({ creating: ! this.state.creating });
  }

  render() {
    return (
      <div className={styles.geofenceSettings}>
        <a onClick={() => this.onCancelClick()}>
          <i className="icon close"></i>
        </a><b>Geofence</b>
        <div className="right floated">
          <button className="ui mini button primary" onClick={() => this.onNewClick()}>New</button>
        </div>
        <GeofenceCreate visible={this.state.creating}/>
        <div className="ui secondary segment">
          <GeofenceList items={[]}/>
        </div>
      </div>
    );
  }
}

export default GeofenceSettings;
