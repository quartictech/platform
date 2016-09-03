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
    for (var layer of this.props.layers) {
      let key="layer_" + layer.id;
      rows.push(<LayerListItem layer={layer} layerToggleVisible={this.props.layerToggleVisible} key={key}/>);
    }
    rows.push(<BucketLayerItem key="bucket"/>)
    return (
      <div className="ui wide sidebar visible">
      <div className="ui basic segment">
        <div className="ui cards">
          {rows}
        </div>
        </div>
      </div>
    );
  }
}

LayerList.propTypes = {
  layers: React.PropTypes.array,
  layerToggleVisible: React.PropTypes.func
}

export default LayerList;
