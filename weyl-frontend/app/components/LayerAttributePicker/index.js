/**
*
* LayerAttributePicker
*
*/

import React from "react";
const $ = require("jquery");

import styles from "./styles.css";
import classNames from "classnames";

class LayerAttributePicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $(this.dropdown)
    .dropdown({
      onChange: (value) => {
        this.props.onChange(value);
      },
    });
  }

  onChange(value) {
    this.props.onChange(value);
  }

  componentWillReceiveProps(nextProps) {

    if (nextProps.selected != "" && nextProps.attributes.findIndex(x => nextProps.selected === x) == -1) {
      $(this.dropdown).dropdown("restore defaults");
    }
  }

  render() {
    const rows = [];
    for (const attribute of this.props.attributes) {
      const itemClasses = classNames("item active", { "selected": attribute === this.props.selected });
      rows.push(<div className={itemClasses} key={attribute} data-value={attribute}>{attribute}</div>);
    }

    return (
      <div className={styles.attributePicker}>
        <div className="ui fluid mini icon labeled search dropdown button" ref={(x) => this.dropdown = x}>
          <i className="tag icon"></i>
          <span className="text">{this.props.selected ? this.props.selected : "Pick Attribute"}</span>
          <div className="menu">
                {rows}
            </div>
          </div>
        </div>
    );
  }
}

export default LayerAttributePicker;
