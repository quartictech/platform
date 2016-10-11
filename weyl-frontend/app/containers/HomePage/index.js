import React from "react";
import Map from "../../components/Map";
import Toolbar from "../../components/Toolbar";
import LayerListPane from "../../components/LayerListPane";
import SelectionPane from "../../components/SelectionPane";
import MapInfo from "../../components/MapInfo";
import FeedPane from "../../components/FeedPane";
import ConnectionStatus from "../../components/ConnectionStatus";
import Chart from "../../components/Chart";

import styles from "./styles.css";

import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as actions from "./actions";
import * as selectors from "./selectors";

class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function

  render() {
    return (
      <div className={styles.container}>
        <ConnectionStatus connectionUp={this.props.connectionUp} />

        <Toolbar
          onSearch={this.props.onSearch}
          onSelect={this.props.onSelect}
          ui={this.props.ui}
          onUiToggle={this.props.onUiToggle}
        />
        <div className={styles.mapContainer}>
          <Map
            layers={this.props.layers}
            onMapLoading={this.props.onMapLoading}
            onMapLoaded={this.props.onMapLoaded}
            onMouseMove={this.props.onMapMouseMove}
            onMouseClick={this.props.onMapMouseClick}
            selection={this.props.selectionIds}
            map={this.props.map}
            geofence={this.props.geofence}
            onGeofenceChange={this.props.onGeofenceChange}
          />
        </div>

        <div className={styles.leftDrawer}>
          <LayerListPane
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
          <SelectionPane
            selectedFeaturedIds={this.props.selectionIds}
            selectionInfo={this.props.selectionInfo}
            layers={this.props.layers}
            onClose={this.props.onSelectionClose}
          />
          <FeedPane
            feed={this.props.feed}
            visible={this.props.ui.panels.liveFeed}
            onUiToggle={this.props.onUiToggle}
            layers={this.props.layers}
          />
        </div>

        <div className={styles.bottomDrawer}>
          <Chart
            visible={this.props.ui.panels.chart}
            timeSeries={this.props.timeSeries}
            onUiToggle={this.props.onUiToggle}
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
  layers: React.PropTypes.object,
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
};

const mapDispatchToProps = {
  onSearch: actions.search,
  onSelect: actions.layerCreate,
  layerToggleVisible: actions.layerToggleVisible,
  layerClose: actions.layerClose,
  onBucketCompute: actions.bucketComputation,
  onUiToggle: actions.toggleUi,
  onSelectionClose: actions.clearSelection,
  onLayerStyleChange: actions.layerSetStyle,
  onToggleValueVisible: actions.layerToggleValueVisible,
  onMapLoading: actions.mapLoading,
  onMapLoaded: actions.mapLoaded,
  onMapMouseMove: actions.mapMouseMove,
  onMapMouseClick: actions.mapMouseClick,
  onGeofenceEdit: actions.geofenceEditStart,
  onGeofenceSave: actions.geofenceEditFinish,
  onGeofenceChange: actions.geofenceEditChange,
  onGeofenceChangeType: actions.geofenceChangeType,
};

const mapStateToProps = createStructuredSelector({
  layers: selectors.selectLayers(),
  ui: selectors.selectUi(),
  selectionIds: selectors.selectSelectionIds(),
  selectionInfo: selectors.selectSelectionInfo(),
  timeSeries: selectors.selectTimeSeries(),
  map: selectors.selectMap(),
  geofence: selectors.selectGeofence(),
  feed: selectors.selectFeed(),
  connectionUp: selectors.selectConnectionUp(),
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
