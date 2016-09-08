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

import LayerPicker from '../LayerPicker';

class BucketLayerItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {selectedLayer: null, selectedBuckets: null};
  }
  componentDidMount() {
    $('#aggregate-dropdown')
    .dropdown();
  }

  onLayerChange(value) {
    this.state.selectedLayer = value;
  }

  onBucketsChange(value) {
    this.state.selectedBuckets = value;
  }

  onCancelClick() {
    this.props.onBucketToggle();
  }

  onComputeClick() {
    console.log("compute click");
    this.props.onCompute({
      aggregationPropertyName: "test",
      features: this.state.selectedLayer,
      buckets: this.state.selectedBuckets,
      aggregation: {
        type: "count"
      }
    });
  }

  render() {
    return (
        <div className={styles.bucketLayerItem}>
        <div className="ui raised fluid card">
          <div className="content">
            <div className="header">Bucket Layer </div>
            <div className="meta">Take geographical features from one layer and assign them to polygons of another, aggregating the results.</div>
          </div>
          <div className="content">
          <form className="ui form">
            <div className="field">
              <LayerPicker layers={this.props.layers} label="Pick Layer" onChange={this.onLayerChange.bind(this)}/>
            </div>
            <div className="field">
              <LayerPicker layers={this.props.layers} label="Pick Buckets" onChange={this.onBucketsChange.bind(this)}/>
            </div>

            <div className="field">
              <div id="aggregate-dropdown" className="ui floating labeled icon dropdown button">
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
            <button className="ui button primary" onClick={this.onComputeClick.bind(this)}>Compute</button>
            <button className="ui button red" onClick={this.onCancelClick.bind(this)}>Cancel</button>
          </div>
        </div>
      </div>
    );
  }
}

BucketLayerItem.PropTypes = {
  onCompute: React.PropTypes.func,
  onBucketToggle: React.PropTypes.func
}

export default BucketLayerItem;
