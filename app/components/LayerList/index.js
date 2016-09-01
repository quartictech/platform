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

var $ = require('jquery');

class LayerList extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $('.ui.sidebar')
      .sidebar({
        context: $('#container')
      });
    }

    componentDidUpdate() {
      $('.ui.dropdown')
      .dropdown();
  }

  render() {
    let rows = [];
    for (var layer of this.props.layers) {
      rows.push(<LayerListItem layer={layer} layerVisibleToggle={this.props.layerVisibleToggle}/>);
    }
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
  layerVisibleToggle: React.PropTypes.func
}

export default LayerList;
