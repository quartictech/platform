/**
*
* LayerPicker
*
*/

import React from "react";

import styles from "./styles.css";
const $ = require("jquery");
const _ = require("underscore");

class LayerPicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $(this.dropdown)
    .dropdown({
      onChange: (value) => {
        console.log(value);
        this.props.onChange(value);
      },
    });
  }

  onChange(value) {
    this.props.onChange(value);
  }

  render() {
    const rows = [];

    _.values(this.props.layers).forEach(layer =>
      rows.push(<div className="item" key={layer.id} data-value={layer.id}>{layer.metadata.name}</div>)
    );

    return (
      <div className={styles.layerPicker}>
        <div className="ui mini floating labeled icon dropdown button" ref={(x) => (this.dropdown = x)}>
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

export default LayerPicker;
