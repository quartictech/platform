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
import DetailsTablePane from "../../components/DetailsTablePane";
import styles from "./styles.css";

import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as actions from "./actions";
import * as selectors from "./selectors";

class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    const noLayers = this.props.layers.isEmpty();

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
              onGeofenceSetManualGeometry={this.props.onGeofenceSetManualGeometry}
            />
          </div>
        </div>

        <div className={styles.topDrawer}>
          <Toolbar
            layerList={this.props.layerList}
            onSelectLayer={this.props.onSelectLayer}
            ui={this.props.ui}
            onUiToggle={this.props.onUiToggle}
            geofencePaneVisible={this.props.geofence.paneVisible}
            onGeofencePaneToggle={this.props.onGeofencePaneToggle}
            onSetTheme={this.props.onSetTheme}
            buttonsDisabled={noLayers}
          />
        </div>

        <div className={styles.leftDrawer}>
          <ComputePane
            layers={this.props.layers}
            computation={this.props.computation}
            onComputationStart={this.props.onComputationStart}
            onClose={() => this.props.onUiToggle("calculate")}
            visible={!noLayers && (this.props.ui.layerOp === "calculate")}
          />

          <GeofenceSettingsPane
            layers={this.props.layers}
            geofence={this.props.geofence}
            onSetManualControlsVisibility={this.props.onGeofenceSetManualControlsVisibility}
            onCommitSettings={this.props.onGeofenceCommitSettings}
            onToggleAlerts={this.props.onGeofenceToggleAlerts}
            onClose={this.props.onGeofencePaneToggle}
            visible={!noLayers && this.props.geofence.paneVisible}
          />

          <LayerListPane
            layers={this.props.layers}
            layerToggleVisible={this.props.layerToggleVisible}
            onLayerStyleChange={this.props.onLayerStyleChange}
            layerClose={this.props.layerClose}
            onToggleValueVisible={this.props.onToggleValueVisible}
            onToggleAllValuesVisible={this.props.onToggleAllValuesVisible}
            onApplyTimeRangeFilter={this.props.onApplyTimeRangeFilter}
            onClose={() => this.props.onUiToggle("layerList")}
            visible={!noLayers && this.props.ui.panels.layerList}
            onLayerExport={this.props.onLayerExport}
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
            timeSeries={this.props.chart.toJS().data}
            onUiToggle={this.props.onUiToggle}
            visible={!noLayers && this.props.ui.panels.chart}
          />
          <DetailsTablePane
            details={this.props.details.toJS().data}
            onUiToggle={this.props.onUiToggle}
            visible={!noLayers && this.props.ui.panels.table}
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
  onSelectLayer: actions.layerCreate,
  layerToggleVisible: actions.layerToggleVisible,
  layerClose: actions.layerClose,
  onComputationStart: actions.computationStart,
  onUiToggle: actions.toggleUi,
  onGeofencePaneToggle: actions.geofencePaneToggleVisibility,
  onSetTheme: actions.uiSetTheme,
  onSelectionClose: actions.clearSelection,
  onLayerStyleChange: actions.layerSetStyle,
  onToggleValueVisible: actions.layerToggleValueVisible,
  onToggleAllValuesVisible: actions.layerToggleAllValuesVisible,
  onApplyTimeRangeFilter: actions.layerApplyTimeRangeFilter,
  onMapLoading: actions.mapLoading,
  onMapLoaded: actions.mapLoaded,
  onMapMouseMove: actions.mapMouseMove,
  onMapMouseClick: actions.mapMouseClick,
  onGeofenceSetManualControlsVisibility: actions.geofenceSetManualControlsVisibility,
  onGeofenceCommitSettings: actions.geofenceCommitSettings,
  onGeofenceSetManualGeometry: actions.geofenceSetManualGeometry,
  onGeofenceToggleAlerts: actions.geofenceToggleAlerts,
  onLayerExport: actions.layerExport,
};

const mapStateToProps = createStructuredSelector({
  layerList: selectors.selectLayerList,
  layers: selectors.selectLayers,
  ui: selectors.selectUi,
  selection: selectors.selectSelection,
  computation: selectors.selectComputation,
  map: selectors.selectMap,
  geofence: selectors.selectGeofence,
  chart: selectors.selectChart,
  details: selectors.selectDetails,
  histograms: selectors.selectHistograms,
  attributes: selectors.selectAttributes,
  connectionUp: selectors.selectConnectionUp,
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
