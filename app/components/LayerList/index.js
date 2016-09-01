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
      });
    }

    componentDidUpdate() {
      $('.ui.dropdown')
      .dropdown();
  }

  render() {
    let rows = [];
    for (var layer of this.props.layers) {
      let buttonClassNames = classNames("ui toggle compact button icon left attached", {"active": layer.visible});
      rows.push(<div className="item" key={layer.id}>
      <div className="left floated content">
      <button className={buttonClassNames} onClick={this.props.layerVisibleToggle} id={layer.id}>
      <i className="icon eye"></i>
      </button>
      <div className="ui compact dropdown button right attached icon secondary">
      <i className="icon paint brush"></i>
      <div className="menu">
  <div className="item">

      <i className="dropdown icon"></i>
  Fill
    <div className="menu">
    <div className="item">Choropleth</div>
    </div>
  </div>
  <div className="item">Stroke</div>
  </div>
      </div>
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

      <div className="ui">
        <div className="ui very relaxed celled list">
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
