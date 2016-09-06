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
import { polygonLayerStyle } from './styles.js';

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
    console.log(nextProps);
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
          "id": layer.id + "_polygon",
          "type": "fill",
          "source": layer.id,
          "source-layer": layer.id + "_polygon",
          'paint': polygonLayerStyle(layer),
        });

        this.state.map.addLayer({
          "id": layer.id + "_point",
          "type": "circle",
          "source": layer.id,
          "source-layer": layer.id + "_point",
          'paint': {
            // make circles larger as the user zooms from z12 to z22
            'circle-radius': 1.75,
            // color circles by ethnicity, using data-driven styles
            'circle-color': '#223b53'
          }
          });
      }
      this.state.map.setLayoutProperty(layer.id + "_polygon", "visibility", layer.visible ? "visible" : "none");
      this.state.map.setLayoutProperty(layer.id + "_point", "visibility", layer.visible ? "visible" : "none");
    })
  }

  render() {
    let mapStyle={width: this.props.size.width + "px", height: this.props.size.height + "px"};
    console.log(mapStyle);
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

export default SizeMe({monitorHeight:true})(Map);
