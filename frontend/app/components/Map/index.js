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

import { line_color_stops, tube_color_stops } from './lines_colors.js';

class Map extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {map: null, width: 0, height: 0 };
    this.state.visibleLayerIds = [];
  }

  onMouseMove(e) {
    var features = this.state.map.queryRenderedFeatures(e.point, {layers: this.state.visibleLayerIds});
    this.state.map.getCanvas().style.cursor = (features.length) ? 'pointer' : '';
  }

  onMouseClick(e) {
    var features = this.state.map.queryRenderedFeatures(e.point, {layers: this.state.visibleLayerIds});
    if (!features.length) {
        return;
    }

    var feature = features[0];
    this.props.onSelectFeatures([[feature.layer.source, feature.properties._id]], features);
  }

  componentDidMount() {
    this.state.map = new mapboxgl.Map({
        container: 'map-inner',
        style: this.props.map.style,
        zoom: 9.7,
        center: [-0.10, 51.4800]
    });

    this.state.map.on('mousemove', this.onMouseMove.bind(this));
    this.state.map.on('click', this.onMouseClick.bind(this));
    this.state.map.on('style.load', () => {
      this.props.onMapLoaded();
      this.updateState(this.props);
    });
  }

  createNewLayer(layer) {
    this.state.map.addSource(layer.id, {
      "type": "vector",
      "tiles": ["http://localhost:8080/api/" + layer.id + "/{z}/{x}/{y}.pbf"]
    });

    this.state.map.addLayer({
      "id": layer.id + "_polygon",
      "type": "fill",
      "source": layer.id,
      "source-layer": layer.id + "_polygon",
      'paint': polygonLayerStyle(layer),
    });

    this.state.map.addLayer({
      "id": layer.id + "_polygon_sel",
      "type": "fill",
      "source": layer.id,
      "source-layer": layer.id + "_polygon",
      'paint': {
        "fill-outline-color": "#484896",
        "fill-color": "#FFB85F",//"#6e599f",
        "fill-opacity": 0.75
      },
      "filter": ["in", "FIPS", ""]
    });

    this.state.map.addLayer({
      "id": layer.id + "_point",
      "type": "circle",
      "source": layer.id,
      "source-layer": layer.id + "_point",
      'paint': {
        'circle-radius': 6,
        'circle-color': '#223b53'
      }
    });

    this.state.map.addLayer({
      "id": layer.id + "_line",
      "type": "line",
      "source": layer.id,
      "source-layer": layer.id + "_line",
      'paint': {
        "line-color": {
          property: "name",
          stops: tube_color_stops,
          type: "categorical"
        },
        "line-width": 8
      }
    });
  }

  createValueFilter(spec) {
    let filter = ["none"];
    for (var attribute in spec) {
      let values = spec[attribute];

      if (values.length > 0) {
        console.log(attribute);
        let partial = values.reduce((f, v) => {
          f.push(v);
          return f;
        }, ['in', attribute]);

        filter.push(partial);
      }
    }
    return filter;
  }

  createSelectionFilter(selection, layerId) {
    let filter = ["in", "_id", ""];
    if (selection.hasOwnProperty(layerId)) {
      return selection[layerId].reduce((f, v) => {
        f.push(v);
        return f;
      }, ['in', '_id']);
    } else {
      return ["in", "_id", ""];
    }
  }

  updateState(props) {
    this.state.visibleLayerIds = [];
    props.layers.forEach((layer) => {
      if (layer.visible) {
        this.state.visibleLayerIds.push(layer.id + "_polygon");
        this.state.visibleLayerIds.push(layer.id + "_point");
        this.state.visibleLayerIds.push(layer.id + "_line");
      }

      if (this.state.map.getSource(layer.id) === undefined) {
        this.createNewLayer(layer);
      }

      ["polygon", "polygon_sel", "point", "line"].forEach((sub) => {
          this.state.map.setLayoutProperty(layer.id + "_" + sub, "visibility", layer.visible ? "visible" : "none");
      });

      let polyStyle = polygonLayerStyle(layer);
      this.state.map.setPaintProperty(layer.id + "_polygon", "fill-color", polyStyle["fill-color"]);
      this.state.map.setPaintProperty(layer.id + "_polygon", "fill-outline-color", polyStyle["fill-outline-color"]);

      const valueFilter = this.createValueFilter(layer.filter);
      this.state.map.setFilter(layer.id + '_polygon', valueFilter);

      const selectionFilter = this.createSelectionFilter(props.selection, layer.id);
      this.state.map.setFilter(layer.id + '_polygon_sel', ["all", selectionFilter, valueFilter]);
    });
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.map.style !== this.props.map.style) {
      this.props.onMapLoading();
      this.state.map.setStyle(nextProps.map.style);
    } else if (nextProps.map.ready) {
      // Drawing before the map is ready causes sadness (this prop is set indirectly via the MapBox 'style.load' callback)
      this.updateState(nextProps);
    }
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

export default SizeMe({monitorHeight:true})(Map);
