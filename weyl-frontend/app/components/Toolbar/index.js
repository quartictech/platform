import React from "react";
import {
  Button,
  Classes,
  IconContents,
  Menu,
  MenuItem,
  Popover,
  Position,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as $ from "jquery";
import * as _ from "underscore";
import Search from "../Search";
import { mapThemes } from "../../themes";
import styles from "./styles.css";
import logo from "./quartic.svg";

const ThemePicker = ({ selected, onSelect }) => {
  const menu = (
    <Menu>
      {_.map(mapThemes, (theme, key) =>
        <MenuItem
          key={key}
          text={theme.label}
          iconName={theme.icon}
          label={(selected === key) ? IconContents.TICK : ""}
          onClick={() => onSelect(key)}
        />
      )}
    </Menu>
  );

  return (
    <Popover
      content={menu}
      position={Position.BOTTOM_RIGHT}
      popoverClassName={Classes.MINIMAL}
    >
      <Button
        className={Classes.MINIMAL}
        iconName="settings"
      />
    </Popover>
  );
};

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

          <Search
            onSearch={props.onSearch}
            onSelectLayer={props.onSelectLayer}
            onSelectPlace={props.onSelectPlace}
          />

          <span className="pt-navbar-divider"></span>

          <div className={classNames(Classes.BUTTON_GROUP, Classes.MINIMAL)}>
            <Button
              text="Layers"
              iconName="layers"
              className={props.ui.panels.layerList ? Classes.ACTIVE : ""}
              onClick={() => props.onUiToggle("layerList")}
            />

            <Button
              text="Bucket"
              iconName="helper-management"
              className={(props.ui.layerOp === "bucket") ? Classes.ACTIVE : ""}
              onClick={() => props.onUiToggle("bucket")}
            />

            <Button
              text="Geofence"
              iconName="eye-open"
              className={(props.ui.layerOp === "geofence") ? Classes.ACTIVE : ""}
              onClick={() => props.onUiToggle("geofence")}
            />

            <Button
              text="Chart"
              iconName="chart"
              className={props.ui.panels.chart ? Classes.ACTIVE : ""}
              onClick={() => props.onUiToggle("chart")}
            />
          </div>

          <span className="pt-navbar-divider"></span>

          <div className="pt-button-group">
            <ThemePicker
              selected={props.ui.settings.theme}
              onSelect={props.onSetTheme}
            />
          </div>
        </div>
      </nav>
    </div>
  );
}

export default Toolbar;
