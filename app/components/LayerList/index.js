/**
*
* LayerList
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
import classNames from 'classnames';

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
      let buttonClassNames = classNames("ui compact circular ui toggle button icon", {"active": layer.visible});
      console.log(buttonClassNames);
      rows.push(<div className="item" key={layer.id}>
      <div className="left floated content">
      <button className={buttonClassNames} onClick={this.props.layerVisibleToggle} id={layer.id}>
      <i className="icon eye"></i>
      </button>
      </div>
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
      <div className="ui">
        <div className="ui relaxed celled list">
          {rows}
        </div>
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
