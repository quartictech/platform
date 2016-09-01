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

import SizeMe from 'react-sizeme';

class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {map: null, width: 0, height: 0 };
  }

  componentDidMount() {
    this.state.map = new mapboxgl.Map({
        container: 'map-inner',
        style: 'mapbox://styles/mapbox/light-v9',
        zoom: 13,
        center: [0.1278, 51.5074]
    });
  }

  componentWillReceiveProps(nextProps) {
    nextProps.layers.forEach((layer) => {
      if (this.state.map.getSource(layer.id) === undefined) {
        this.state.map.addSource(layer.id, {
          "type": "vector",
          "tiles": ["http://localhost:8080/api/" + layer.id + "/{z}/{x}/{y}.pbf"]
        });

        // this.state.map.addLayer({
        //   "id": layer.id,
        //   "type": "fill",
        //   "source": layer.id,
        //   "source-layer": "test", // Should be changed to the real layer name!
        //   "paint": {
        //     "fill-color": '#EED322',
        //     'fill-opacity': 0.75
        //   }
        // });

        this.state.map.addLayer({
    "id": layer.id,
    "type": "fill",
    "source": layer.id,
    "source-layer": layer.id,
    'paint': {
        'fill-color': {
            property: 'count',
            stops: [
                [0, '#F2F12D'],
                [100, '#EED322'],
                [500, '#E6B71E'],
                [1000, '#DA9C20'],
                [2000, '#CA8323'],
                [5000, '#B86B25'],
                [10000, '#A25626'],
                [100000, '#8B4225'],
                [250000, '#723122']
            ]
        },
        'fill-opacity': 0.75
    }
});
      }
    this.state.map.setLayoutProperty(layer.id, "visibility", layer.visible ? "visible" : "none");
    })
  }

  render() {
    return (
      <div className={styles.map}>
        <div id="map-inner" className={styles.mapViewport}>
        </div>
        </div>
    );
  }
}

Map.propTypes = {
  layers: React.PropTypes.array
}

export default SizeMe()(Map);
