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
import { themes } from "../../themes";

function Toolbar(props) {
  const bucketClassNames = classNames("item", { "active": props.ui.layerOp === "bucket" });
  const geofenceClassNames = classNames("item", { "active": props.ui.layerOp === "geofence" });
  const chartClassNames = classNames("item", { "active": props.ui.panels.chart });
  const layerListClassNames = classNames("item", { "active": props.ui.panels.layerList });
  const themeClassNames = classNames("item");
  return (
    <div className={styles.toolbar}>
      <div className="ui menu inverted attached labeled icon">
        <div className="header item">
          <i className="icon map" /><span className={styles.brand}>Quartic</span>
        </div>

        <div className="right menu">
          <a className={themeClassNames} onClick={() => props.onUiToggle("theme")}>
            <i className={`icon ${themes[props.ui.settings.theme].icon}`}></i>
            {themes[props.ui.settings.theme].label}
          </a>
          <a className={layerListClassNames} onClick={() => props.onUiToggle("layerList")}>
            <i className="icon list"></i>
            Layers
          </a>
          <a className={bucketClassNames} onClick={() => props.onUiToggle("bucket")}>
            <i className="icon object group"></i>
            Bucket
          </a>
          <a className={geofenceClassNames} onClick={() => props.onUiToggle("geofence")}>
            <i className="icon crop"></i>
            Geofence
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

export default Toolbar;
