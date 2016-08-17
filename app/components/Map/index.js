/**
*
* Map
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

import mapboxgl from 'mapbox-gl'
import 'mapbox-gl.css'
mapboxgl.accessToken = 'pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw';

class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {map: null};
  }
  componentDidMount() {
    this.state.map = new mapboxgl.Map({
        container: 'map-inner',
        style: 'mapbox://styles/mapbox/light-v9',
        zoom: 13,
        center: [0.1278, 51.5074]
    });
  }
  render() {
    if (this.state != null && this.state.map != null) {
      this.state.map.resize();
    }
    return (
      <div className={styles.map}>
        <div id="map-inner" className={styles.mapViewport}>
        </div>
      </div>
    );
  }
}

export default Map;
