import React from "react";

import Map from "../../components/Map";
import Toolbar from "../../components/Toolbar";
import ComputePane from "../../components/ComputePane";
import GeofenceSettingsPane from "../../components/GeofenceSettingsPane";
import LayerListPane from "../../components/LayerListPane";
import SelectionPane from "../../components/SelectionPane";
import MapInfo from "../../components/MapInfo";
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

        <div className={styles.nonDrawer}>
          <div className={styles.mapContainer}>
            <Map
              layers={this.props.layers.toJS()}
              onMapLoading={this.props.onMapLoading}
              onMapLoaded={this.props.onMapLoaded}
              onMouseMove={this.props.onMapMouseMove}
              onMouseClick={this.props.onMapMouseClick}
              selection={this.props.selection.ids}
              map={this.props.map}
              geofence={this.props.geofence}
              onGeofenceEditSetGeometry={this.props.onGeofenceEditSetGeometry}
            />
          </div>
        </div>

        <div className={styles.topDrawer}>
          <Toolbar
            onSearch={this.props.onSearch}
            onSelectLayer={this.props.onSelectLayer}
            onSelectPlace={this.props.onSelectPlace}
            ui={this.props.ui}
            onUiToggle={this.props.onUiToggle}
            onSetTheme={this.props.onSetTheme}
          />
        </div>

        <div className={styles.leftDrawer}>
          <ComputePane
            layers={this.props.layers}
            onCompute={this.props.onCompute}
            onClose={() => this.props.onUiToggle("calculate")}
            visible={this.props.ui.layerOp === "calculate"}
          />

          <GeofenceSettingsPane
            layers={this.props.layers}
            geofence={this.props.geofence}
            onEdit={{
              start: this.props.onGeofenceEditStart,
              finish: this.props.onGeofenceEditFinish,
              setType: this.props.onGeofenceEditSetType,
              setLayer: this.props.onGeofenceEditSetLayer,
            }}
            onToggleAlerts={this.props.onGeofenceToggleAlerts}
            onClose={() => this.props.onUiToggle("geofence")}
            visible={this.props.ui.layerOp === "geofence"}
          />

          <LayerListPane
            layers={this.props.layers}
            layerToggleVisible={this.props.layerToggleVisible}
            onLayerStyleChange={this.props.onLayerStyleChange}
            layerClose={this.props.layerClose}
            onToggleValueVisible={this.props.onToggleValueVisible}
            onClose={() => this.props.onUiToggle("layerList")}
            visible={this.props.ui.panels.layerList}
          />
        </div>

        <div className={styles.rightDrawer}>
          <SelectionPane
            selection={this.props.selection}
            histograms={this.props.histograms}
            attributes={this.props.attributes.toJS()}
            layers={this.props.layers.toJS()}
            onClose={this.props.onSelectionClose}
          />
        </div>

        <div className={styles.bottomDrawer}>
          <Chart
            visible={this.props.ui.panels.chart}
            timeSeries={this.props.chart.toJS().data}
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

const mapDispatchToProps = {
  onSearch: actions.search,
  onSelectLayer: actions.layerCreate,
  onSelectPlace: actions.mapSetLocation,
  layerToggleVisible: actions.layerToggleVisible,
  layerClose: actions.layerClose,
  onCompute: actions.layerComputation,
  onUiToggle: actions.toggleUi,
  onSetTheme: actions.uiSetTheme,
  onSelectionClose: actions.clearSelection,
  onLayerStyleChange: actions.layerSetStyle,
  onToggleValueVisible: actions.layerToggleValueVisible,
  onMapLoading: actions.mapLoading,
  onMapLoaded: actions.mapLoaded,
  onMapMouseMove: actions.mapMouseMove,
  onMapMouseClick: actions.mapMouseClick,
  onGeofenceEditStart: actions.geofenceEditStart,
  onGeofenceEditFinish: actions.geofenceEditFinish,
  onGeofenceEditSetLayer: actions.geofenceEditSetLayer,
  onGeofenceEditSetType: actions.geofenceEditSetType,
  onGeofenceEditSetGeometry: actions.geofenceEditSetGeometry,
  onGeofenceSetGeometry: actions.geofenceSetGeometry,
  onGeofenceToggleAlerts: actions.geofenceToggleAlerts,
};

const mapStateToProps = createStructuredSelector({
  layers: selectors.selectLayers,
  ui: selectors.selectUi,
  selection: selectors.selectSelection,
  map: selectors.selectMap,
  geofence: selectors.selectGeofence,
  chart: selectors.selectChart,
  histograms: selectors.selectHistograms,
  attributes: selectors.selectAttributes,
  connectionUp: selectors.selectConnectionUp,
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
