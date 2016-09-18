/*
 * HomePage
 *
 * This is the first thing users see of our App, at the '/' route
 *
 * NOTE: while this component should technically be a stateless functional
 * component (SFC), hot reloading does not currently support SFCs. If hot
 * reloading is not a neccessity for you then you can refactor it and remove
 * the linting exception.
 */

import React from 'react';
import { FormattedMessage } from 'react-intl';
import messages from './messages';
import  Map from '../../components/Map';
import  Toolbar from '../../components/Toolbar';
import LineChart from '../../components/LineChart';
import LayerList from '../../components/LayerList';
import SelectionView from '../../components/SelectionView';

import styles from './styles.css';

import { connect } from 'react-redux';
import { createStructuredSelector } from 'reselect';
import { search, layerCreate, layerToggleVisible, layerClose, bucketComputation, toggleUi, selectFeatures, clearSelection, loadNumericAttributes, chartSelectAttribute,
  setLayerStyle, toggleValueVisible
 } from './actions';

import { selectLayers, selectLoading, selectUi, selectSelectionIds, selectSelectionFeatures, selectNumericAttributes, selectHistogramChart } from './selectors';

export default class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div className={styles.container}>
        <Toolbar loading={this.props.loading} onSearch={this.props.onSearch}
        onSelect={this.props.onSelect} ui= {this.props.ui} onUiToggle={this.props.onUiToggle}/>
      <div className={styles.mapContainer}>
        <Map layers={this.props.layers}
          onSelectFeatures={this.props.onSelectFeatures}
          selection={this.props.selectionIds}
          />
      </div>

      <div className={styles.leftDrawer}>
          <LayerList layers={this.props.layers}
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
        <SelectionView selection={this.props.selectionFeatures} onClearSelection={this.props.onClearSelection} />
      </div>

      <div className={styles.bottomDrawer}>
            <LineChart visible={this.props.ui.panels.chart} layers={this.props.layers} onLayerSelection={this.props.onChartLayerSelection}
              onAttributeSelection={this.props.onChartAttributeSelection}
              chart={this.props.histogramChart}
              numericAttributes={this.props.numericAttributes}/>
          </div>
        </div>
    );
  }
}

HomePage.propTypes = {
  layers: React.PropTypes.array,
}

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
    onToggleValueVisible: (layerId, attribute, value) => dispatch(layerToggleValueVisible(layerId, attribute, value))
  };
}

const mapStateToProps = createStructuredSelector({
  layers: selectLayers(),
  loading: selectLoading(),
  ui: selectUi(),
  selectionIds: selectSelectionIds(),
  selectionFeatures: selectSelectionFeatures(),
  numericAttributes: selectNumericAttributes(),
  histogramChart: selectHistogramChart(),
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
