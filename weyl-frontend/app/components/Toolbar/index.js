/**
*
* Toolbar
*
*/

import React from "react";

import "lato-font/css/lato-font.css";
import styles from "./styles.css";

import LayerSearch from "../LayerSearch";
import classNames from "classnames";

function Toolbar(props) {
  const bucketClassNames = classNames("item", { "active": props.ui.layerOp === "bucket" });
  const chartClassNames = classNames("item", { "active": props.ui.panels.chart });
  const layerListClassNames = classNames("item", { "active": props.ui.panels.layerList });
  const satelliteClassNames = classNames("item", { "active": props.ui.settings.satellite });
  return (
    <div className={styles.toolbar}>
      <div className="ui menu inverted attached labeled icon">
        <div className="header item">
          <i className="icon map" /><span className={styles.brand}>Quartic</span>
        </div>

        <div className="right menu">
          <a className={satelliteClassNames} onClick={() => props.onUiToggle("satellite")}>
            <i className="icon rocket"></i>
            Satellite
          </a>
          <a className={layerListClassNames} onClick={() => props.onUiToggle("layerList")}>
            <i className="icon list"></i>
            Layers
          </a>
          <a className={bucketClassNames} onClick={() => props.onUiToggle("bucket")}>
            <i className="icon object group"></i>
            Bucket
          </a>
          <a className={chartClassNames} onClick={() => props.onUiToggle("chart")}>
            <i className="icon line chart"></i>
            Chart
          </a>
          <div className="item">
            <LayerSearch onSearch={props.onSearch} onSelect={props.onSelect} />
          </div>
        </div>
      </div>
    </div>
  );
}

Toolbar.propTypes = {
  onSearch: React.PropTypes.func,
  onSelect: React.PropTypes.func,
  ui: React.PropTypes.object,
  onUiToggle: React.PropTypes.func,
};

export default Toolbar;
