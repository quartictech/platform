/**
*
* LayerList
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
var $ = require('jquery');

class LayerList extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $('.ui.sidebar')
      .sidebar({
        context: $('#container')
      })

  }
  render() {
    let rows = [];
    for (var layer of this.props.layers) {
      rows.push(<div className="item">
      <button className="circular ui icon button">
      <i className="icon eye"></i>
      </button>
        <div className="content">
          <div className="header">
            {layer.name}
          </div>
          {layer.description}
        </div>

        </div>);
    }
    return (
      <div className="ui wide sidebar visible">
      <div className="ui container">
      <h1 className="ui header">
      Layers
      </h1>
      <div className="ui inverted segment">
        <div className="ui inverted relaxed celled list">
          {rows}
        </div>
        </div>
      </div>
      </div>
    );
  }
}

LayerList.propTypes = {
  layers: React.PropTypes.array
}

export default LayerList;
