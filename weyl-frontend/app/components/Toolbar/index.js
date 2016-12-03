import React from "react";

import styles from "./styles.css";

import { Button } from "@blueprintjs/core";
import Search from "../Search";
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
            role="presentation"
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

          <Search
            onSearch={props.onSearch}
            onSelectLayer={props.onSelectLayer}
            onSelectPlace={props.onSelectPlace}
          />
        </div>
      </nav>
    </div>
  );
}

export default Toolbar;
