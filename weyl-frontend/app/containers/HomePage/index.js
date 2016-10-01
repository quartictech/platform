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
import LayerList from "../../components/LayerList";
import SelectionView from "../../components/SelectionView";
import MapInfo from "../../components/MapInfo";

import styles from "./styles.css";

import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as actions from "./actions";
import * as selectors from "./selectors";

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
            geofence={this.props.geofence}
            onGeofenceChange={this.props.onGeofenceChange}
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
            onGeofenceEdit={this.props.onGeofenceEdit}
            onGeofenceSave={this.props.onGeofenceSave}
            geofence={this.props.geofence}
            onGeofenceChangeType={this.props.onGeofenceChangeType}
          />
        </div>

        <div className={styles.rightDrawer}>
          <SelectionView
            selection={this.props.selectionFeatures}
            onClearSelection={this.props.onClearSelection}
          />
        </div>

        <div className={styles.bottomDrawer}>
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

const mapDispatchToProps = {
  onSearch: actions.search,
  onSelect: actions.layerCreate,
  layerToggleVisible: actions.layerToggleVisible,
  layerClose: actions.layerClose,
  onBucketCompute: actions.bucketComputation,
  onUiToggle: actions.toggleUi,
  onSelectFeatures: actions.selectFeatures,
  onClearSelection: actions.clearSelection,
  onChartLayerSelection: actions.loadNumericAttributes,
  onChartAttributeSelection: actions.chartSelectAttribute,
  onLayerStyleChange: actions.layerSetStyle,
  onToggleValueVisible: actions.layerToggleValueVisible,
  onMapLoading: actions.mapLoading,
  onMapLoaded: actions.mapLoaded,
  onMapMouseMove: actions.mapMouseMove,
  onGeofenceEdit: actions.geofenceEditStart,
  onGeofenceSave: actions.geofenceEditFinish,
  onGeofenceChange: actions.geofenceEditChange,
  onGeofenceChangeType: actions.geofenceChangeType,
};

const mapStateToProps = createStructuredSelector({
  layers: selectors.selectLayers(),
  ui: selectors.selectUi(),
  selectionIds: selectors.selectSelectionIds(),
  selectionFeatures: selectors.selectSelectionFeatures(),
  numericAttributes: selectors.selectNumericAttributes(),
  map: selectors.selectMap(),
  geofence: selectors.selectGeofence(),
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
