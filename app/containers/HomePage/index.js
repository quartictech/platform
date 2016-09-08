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

import styles from './styles.css';

import { connect } from 'react-redux';
import { createStructuredSelector } from 'reselect';
import { search, addItem, layerToggleVisible, bucketComputation, toggleUi } from './actions';

import { selectLayers, selectLoading, selectUi } from './selectors';

export default class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div className={styles.container}>
        <Toolbar loading={this.props.loading} onSearch={this.props.onSearch}
        onSelect={this.props.onSelect} ui= {this.props.ui} onUiToggle={this.props.onUiToggle}/>
        <div className={styles.innerContainer}>
          <LayerList layers={this.props.layers}
            layerToggleVisible={this.props.layerToggleVisible}
            onBucketCompute={this.props.onBucketCompute}
            ui={this.props.ui}
            onBucketToggle={this.props.onBucketToggle}
          />
            <div className={styles.innerContainer2}>
            <Map layers={this.props.layers}/>
            <LineChart visible={this.props.ui.panels.chart}/>
            </div>
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
    onSelect: (result) => dispatch(addItem(result)),
    layerToggleVisible: (id) => dispatch(layerToggleVisible(id)),
    onBucketCompute: (computation) => dispatch(bucketComputation(computation)),
    onUiToggle: (element) => dispatch(toggleUi(element))
  };
}

const mapStateToProps = createStructuredSelector({
  layers: selectLayers(),
  loading: selectLoading(),
  ui: selectUi()
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
