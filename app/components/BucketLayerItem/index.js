/**
*
* BucketLayerItem
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
var $ = require('jquery');

class BucketLayerItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $('.ui.dropdown')
    .dropdown();
  }
  render() {
    return (
        <div className="ui raised fluid card">
          <div className="content">
            <div className="header">Bucket Layer </div>
            <div className="meta">Take geographical features from one layer and assign them to polygons of another, aggregating the results.</div>
          </div>
          <div className="content">
          <form className="ui form">
            <div className="field">
              <div className="ui buttons">
                <button className="ui button"> Pick Layer </button>
                <button className="ui button">Pick Buckets</button>
              </div>
            </div>

            <div className="field">
              <div className="ui floating labeled icon dropdown button">
                <i className="filter icon"></i>
                <span className="text">Aggregate</span>
                <div className="menu">
                  <div className="header">
                    <i className="tags icon"></i>
                    Filter by tag
                  </div>
                  <div className="item">
                  Count
                  </div>
                  <div className="item">
                  Sum
                  </div>
                </div>
              </div>
            </div>
          </form>
          </div>
          <div className="ui content">
            <button className="ui button primary">Compute</button>
            <button className="ui button red">Cancel</button>
          </div>
        </div>
    );
  }
}

export default BucketLayerItem;
