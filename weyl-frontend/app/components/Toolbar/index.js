import React from "react";

import "lato-font/css/lato-font.css";
import styles from "./styles.css";

import Search from "../Search";
import classNames from "classnames";
import { mapThemes } from "../../themes";
const $ = require("jquery");
import logo from "./quartic.svg";

function Toolbar(props) {
  const bucketClassNames = classNames("item", { "active": props.ui.layerOp === "bucket" });
  const geofenceClassNames = classNames("item", { "active": props.ui.layerOp === "geofence" });
  const layerListClassNames = classNames("item", { "active": props.ui.panels.layerList });
  const liveFeedClassNames = classNames("item", { "active": props.ui.panels.liveFeed });
  const chartClassNames = classNames("item", { "active": props.ui.panels.chart });
  const themeClassNames = classNames("item");

  const itemStyle = { paddingTop: "7px", paddingBottom: "0px" };

  return (
    <div className={styles.toolbar}>
      <div className="ui raised fluid segment inverted" style={{ padding: "5px" }}>
        <div className="ui small menu compact inverted labeled icon">
          <div className="header item" style={itemStyle}>

            <img
              className={styles.logo}
              src={logo}
              role="presentation"
              data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
              data-variation="mini"
              ref={x => $(x).popup()}
            >
            </img>
            <span className={styles.brand}>Quartic</span>
          </div>

          <div className="right menu">
            <a className={themeClassNames} onClick={() => props.onUiToggle("theme")} style={itemStyle}>
              <i className={`icon ${mapThemes[props.ui.settings.theme].icon}`}></i>
              {mapThemes[props.ui.settings.theme].label}
            </a>
            <a className={layerListClassNames} onClick={() => props.onUiToggle("layerList")} style={itemStyle}>
              <i className="icon list"></i>
              Layers
            </a>
            <a className={bucketClassNames} onClick={() => props.onUiToggle("bucket")} style={itemStyle}>
              <i className="icon object group"></i>
              Bucket
            </a>
            <a className={geofenceClassNames} onClick={() => props.onUiToggle("geofence")} style={itemStyle}>
              <i className="icon crop"></i>
              Geofence
            </a>
            <a className={chartClassNames} onClick={() => props.onUiToggle("chart")} style={itemStyle}>
              <i className="icon area chart"></i>
              Chart
            </a>
            <a className={liveFeedClassNames} onClick={() => props.onUiToggle("liveFeed")} style={itemStyle}>
              <i className="icon feed"></i>
              Live Feed
            </a>
            <div className="item" style={itemStyle}>
              <Search
                onSearch={props.onSearch}
                onSelectLayer={props.onSelectLayer}
                onSelectPlace={props.onSelectPlace}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Toolbar;
