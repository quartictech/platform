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

          <Button
            text="Layers"
            iconName="layers"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: props.ui.panels.layerList })}
            onClick={() => props.onUiToggle("layerList")}
          />

          <Button
            text="Bucket"
            iconName="helper-management"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: (props.ui.layerOp === "bucket") })}
            onClick={() => props.onUiToggle("bucket")}
          />

          <Button
            text="Geofence"
            iconName="eye-open"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: (props.ui.layerOp === "geofence") })}
            onClick={() => props.onUiToggle("geofence")}
          />

          <Button
            text="Chart"
            iconName="chart"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: props.ui.panels.chart })}
            onClick={() => props.onUiToggle("chart")}
          />

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
