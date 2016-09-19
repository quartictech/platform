/**
*
* LayerPicker
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
var $ = require('jquery');

class LayerPicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    console.log(this.dropdown);
    $(this.dropdown)
    .dropdown({
       onChange: (value) => {
         console.log(value);
         this.props.onChange(value);
       }
     });
  }

  onChange(value) {
    this.props.onChange(value);
  }

  render() {
    let rows = [];

    for (var layer of this.props.layers) {
      rows.push(<div className="item" key={layer.id} data-value={layer.id}>{layer.name}</div>);
    }

    return (
      <div className={styles.layerPicker}>
        <div className="ui floating labeled icon dropdown button" ref={(x) => this.dropdown = x}>
                <i className="filter icon"></i>
                <span className="text">{this.props.label}</span>
                <div className="menu">
                  {rows}
                </div>
              </div>
      </div>
    );
  }
}

LayerPicker.PropTypes = {
  label: React.PropTypes.string,
  layers: React.PropTypes.array,
  onChange: React.PropTypes.func
}

export default LayerPicker;
