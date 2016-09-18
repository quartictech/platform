/**
*
* LayerList
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

import LayerListItem from '../LayerListItem';
import BucketLayerItem from '../BucketLayerItem';

var $ = require('jquery');

class LayerList extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $('.ui.sidebar')
      .sidebar({
        context: $('#container')
      });
    }

  render() {
    let rows = [];
    if (this.props.ui.layerOp == "bucket") {
      rows.push(<BucketLayerItem onCompute={this.props.onBucketCompute} layers={this.props.layers} onUiToggle={this.props.onUiToggle} key="bucket"/>)
    }
    for (var layer of this.props.layers) {
      if (layer.closed) {
        continue;
      }
      let key="layer_" + layer.id;
      rows.push(<LayerListItem layer={layer} layerToggleVisible={this.props.layerToggleVisible} key={key}
        onLayerStyleChange={this.props.onLayerStyleChange}
        layerClose={this.props.layerClose}
        />);
    }

    if (this.props.layers.length > 0) {
      return (
        <div className={styles.layerList} style={{"visibility": this.props.visible ? "visible" : "hidden"}}>
          <div className="ui raised fluid card">
            <div className={styles.innerLayerList}>
              {rows}
            </div>
          </div>
        </div>
      );
    }
    else {
      return null;
    }
  }
}

LayerList.propTypes = {
  layers: React.PropTypes.array,
  layerToggleVisible: React.PropTypes.func,
  layerClose: React.PropTypes.func,
  onBucketCompute: React.PropTypes.func,
  ui: React.PropTypes.object,
  onUiToggle: React.PropTypes.func,
  onLayerStyleChange: React.PropTypes.func
}

export default LayerList;
