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

import styles from './styles.css';

import { connect } from 'react-redux';
import { createStructuredSelector } from 'reselect';
import { importLayer } from './actions';

import { selectLayers } from './selectors';

export default class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div className={styles.container}>
        <Toolbar importLayerClick={this.props.importLayer}/>
        <Map layers={this.props.layers}/>
      </div>
    );
  }
}

HomePage.propTypes = {
  layers: React.PropTypes.array,
}

function mapDispatchToProps(dispatch) {
  return {
    importLayer: (evt) => dispatch(importLayer()),
    dispatch,
  };
}

const mapStateToProps = createStructuredSelector({
  layers: selectLayers(),
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
