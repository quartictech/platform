import React from "react";

import styles from "./styles.css";

import { Button } from "@blueprintjs/core";
import Search from "../Search";
import classNames from "classnames";
import { mapThemes } from "../../themes";
const $ = require("jquery");
import logo from "./quartic.svg";

function Toolbar(props) {
  return (
    <div className={styles.toolbar}>
      <nav className="pt-navbar pt-dark">
        <div className="pt-navbar-group pt-align-left">
          <img
            className={styles.logo}
            src={logo}
            data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
            data-variation="mini"
            ref={x => $(x).popup()}
          />
          <span className="pt-navbar-divider"></span>

          <div className="pt-button-group">
            <Button
              text={mapThemes[props.ui.settings.theme].label}
              iconName={mapThemes[props.ui.settings.theme].icon}
              onClick={() => props.onUiToggle("theme")}
            />

            <Button
              text="Layers"
              iconName="layers"
              className={props.ui.panels.layerList ? "pt-active" : ""}
              onClick={() => props.onUiToggle("layerList")}
            />

            <Button
              text="Bucket"
              iconName="helper-management"
              className={(props.ui.layerOp === "bucket") ? "pt-active" : ""}
              onClick={() => props.onUiToggle("bucket")}
            />

            <Button
              text="Geofence"
              iconName="eye-open"
              className={(props.ui.layerOp === "geofence") ? "pt-active" : ""}
              onClick={() => props.onUiToggle("geofence")}
            />

            <Button
              text="Chart"
              iconName="chart"
              className={props.ui.panels.chart ? "pt-active" : ""}
              onClick={() => props.onUiToggle("chart")}
            />
          </div>

          <span className="pt-navbar-divider"></span>

          <input
            className="pt-input"
            placeholder="Search datasets..."
            type="text"
          />
        </div>
      </nav>
    </div>
  );
}

export default Toolbar;


// <div className={styles.toolbar}>
//       <div className="ui raised fluid segment inverted" style={{ padding: "5px" }}>
//         <div className="ui small menu compact inverted labeled icon">
//           <div className="header item" style={itemStyle}>
//
//             <img
//               className={styles.logo}
//               src={logo}
//               role="presentation"
//               data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
//               data-variation="mini"
//               ref={x => $(x).popup()}
//             >
//             </img>
//             <span className={styles.brand}>Quartic</span>
//           </div>
//
//           <div className="right menu">
//             <a className={themeClassNames} onClick={() => props.onUiToggle("theme")} style={itemStyle}>
//               <i className={`icon ${mapThemes[props.ui.settings.theme].icon}`}></i>
//               {mapThemes[props.ui.settings.theme].label}
//             </a>
//             <a className={layerListClassNames} onClick={() => props.onUiToggle("layerList")} style={itemStyle}>
//               <i className="icon list"></i>
//               Layers
//             </a>
//             <a className={bucketClassNames} onClick={() => props.onUiToggle("bucket")} style={itemStyle}>
//               <i className="icon object group"></i>
//               Bucket
//             </a>
//             <a className={geofenceClassNames} onClick={() => props.onUiToggle("geofence")} style={itemStyle}>
//               <i className="icon crop"></i>
//               Geofence
//             </a>
//             <a className={chartClassNames} onClick={() => props.onUiToggle("chart")} style={itemStyle}>
//               <i className="icon area chart"></i>
//               Chart
//             </a>
//             <div className="item" style={itemStyle}>
//               <Search
//                 onSearch={props.onSearch}
//                 onSelectLayer={props.onSelectLayer}
//                 onSelectPlace={props.onSelectPlace}
//               />
//             </div>
//           </div>
//         </div>
//       </div>
//     </div>
