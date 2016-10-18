import React from "react";

import styles from "./styles.css";
const $ = require("jquery");
import classNames from "classnames";

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
    const rows = this.props.layers.toArray().map(layer => (
      <div className="item" key={layer.get("id")} data-value={layer.get("id")}>
        {layer.getIn(["metadata", "name"])}
      </div>
    ));


    const dropdownClasses = classNames("ui", "mini", "floating", "labeled",
      "icon", "dropdown", "button", { "disabled": this.props.layers.size === 0 });
    return (
      <div className={styles.layerPicker}>
        <div className={dropdownClasses} ref={(x) => (this.dropdown = x)}>
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
