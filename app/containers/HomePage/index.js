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
import LayerList from '../../components/LayerList';

import styles from './styles.css';

import { connect } from 'react-redux';
import { createStructuredSelector } from 'reselect';
import { search, addItem } from './actions';

import { selectLayers, selectLoading } from './selectors';

export default class HomePage extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (

      <div className={styles.container}>
        <Toolbar loading={this.props.loading} onSearch={this.props.onSearch} onSelect={this.props.onSelect}/>
        <div id="container" className={styles.container}>
          <LayerList layers={this.props.layers} />
          <div className="pusher">
            <Map layers={this.props.layers}/>
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
    onSelect: (result) => dispatch(addItem(result))
  };
}

const mapStateToProps = createStructuredSelector({
  layers: selectLayers(),
  loading: selectLoading()
});

// Wrap the component to inject dispatch and state into it
export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
