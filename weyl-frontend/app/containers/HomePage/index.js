/*
 * HomePage
 *
 * This is the first thing users see of our App, at the "/" route
 *
 * NOTE: while this component should technically be a stateless functional
 * component (SFC), hot reloading does not currently support SFCs. If hot
 * reloading is not a neccessity for you then you can refactor it and remove
 * the linting exception.
 */

import React from "react";
import Map from "../../components/Map";
import Toolbar from "../../components/Toolbar";
import LineChart from "../../components/LineChart";
import LayerList from "../../components/LayerList";
import SelectionView from "../../components/SelectionView";
import MapInfo from "../../components/MapInfo";

import styles from "./styles.css";

import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import { search, layerCreate, layerToggleVisible, layerClose, bucketComputation, toggleUi, selectFeatures, clearSelection, loadNumericAttributes, chartSelectAttribute,
  setLayerStyle, layerToggleValueVisible, mapLoading, mapLoaded, mapMouseMove
 } from "./actions";

import { selectLayers, selectUi, selectSelectionIds, selectSelectionFeatures, selectNumericAttributes, selectMap } from "./selectors";

class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div className={styles.container}>
        <Toolbar
          onSearch={this.props.onSearch}
          onSelect={this.props.onSelect}
          ui={this.props.ui}
          onUiToggle={this.props.onUiToggle}
        />
        <div className={styles.mapContainer}>
          <Map
            layers={this.props.layers}
            onSelectFeatures={this.props.onSelectFeatures}
            onMapLoading={this.props.onMapLoading}
            onMapLoaded={this.props.onMapLoaded}
            onMouseMove={this.props.onMapMouseMove}
            selection={this.props.selectionIds}
            map={this.props.map}
          />
        </div>

        <div className={styles.leftDrawer}>
          <LayerList
            layers={this.props.layers}
            layerToggleVisible={this.props.layerToggleVisible}
            onBucketCompute={this.props.onBucketCompute}
            ui={this.props.ui}
            visible={this.props.ui.panels.layerList}
            onUiToggle={this.props.onUiToggle}
            onLayerStyleChange={this.props.onLayerStyleChange}
            layerClose={this.props.layerClose}
            onToggleValueVisible={this.props.onToggleValueVisible}
          />
        </div>

        <div className={styles.rightDrawer}>
          <SelectionView
            selection={this.props.selectionFeatures}
            onClearSelection={this.props.onClearSelection}
          />
        </div>

        <div className={styles.bottomDrawer}>
          <LineChart
            visible={this.props.ui.panels.chart}
            layers={this.props.layers}
            onLayerSelection={this.props.onChartLayerSelection}
            numericAttributes={this.props.numericAttributes}
          />
        </div>

        <div className={styles.infoBar}>
          <MapInfo
            map={this.props.map}
          />
        </div>
      </div>
    );
  }
}

HomePage.propTypes = {
  layers: React.PropTypes.array,
  layerToggleVisible: React.PropTypes.func,
  onSearch: React.PropTypes.func,
  onSelect: React.PropTypes.func,
  onUiToggle: React.PropTypes.func,
  onSelectFeatures: React.PropTypes.func,
  onMapLoading: React.PropTypes.func,
  onMapLoaded: React.PropTypes.func,
  ui: React.PropTypes.object,
  selectionIds: React.PropTypes.object,
  map: React.PropTypes.object,
  onBucketCompute: React.PropTypes.func,
  onLayerStyleChange: React.PropTypes.func,
  layerClose: React.PropTypes.func,
  onToggleValueVisible: React.PropTypes.func,
  selectionFeatures: React.PropTypes.array,
  onClearSelection: React.PropTypes.func,
  numericAttributes: React.PropTypes.object,
};

function mapDispatchToProps(dispatch) {
  return {
    dispatch,
    onSearch: (query, callback) => dispatch(search(query, callback)),
    onSelect: (result) => dispatch(layerCreate(result)),
    layerToggleVisible: (id) => dispatch(layerToggleVisible(id)),
    layerClose: (id) => dispatch(layerClose(id)),
    onBucketCompute: (computation) => dispatch(bucketComputation(computation)),
    onUiToggle: (element) => dispatch(toggleUi(element)),
    onSelectFeatures: (ids, features) => dispatch(selectFeatures(ids, features)),
    onClearSelection: () => dispatch(clearSelection()),
    onChartLayerSelection: (layerId) => dispatch(loadNumericAttributes(layerId)),
    onChartAttributeSelection: (attribute) => dispatch(chartSelectAttribute(attribute)),
    onLayerStyleChange: (layerId, style) => dispatch(setLayerStyle(layerId, style)),
    onToggleValueVisible: (layerId, attribute, value) => dispatch(layerToggleValueVisible(layerId, attribute, value)),
    onMapLoading: () => dispatch(mapLoading()),
    onMapLoaded: () => dispatch(mapLoaded()),
    onMapMouseMove: (location) => dispatch(mapMouseMove(location))
  };
}

const mapStateToProps = createStructuredSelector({
  layers: selectLayers(),
  ui: selectUi(),
  selectionIds: selectSelectionIds(),
  selectionFeatures: selectSelectionFeatures(),
  numericAttributes: selectNumericAttributes(),
  map: selectMap(),
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
