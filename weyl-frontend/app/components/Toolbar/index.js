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
import * as _ from "underscore";
import Search from "../Search";
import { mapThemes } from "../../themes";
import styles from "./styles.css";
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
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: props.ui.panels.layerList && !props.buttonsDisabled })}
            onClick={() => props.onUiToggle("layerList")}
            disabled={props.buttonsDisabled}
          />

          <Button
            text="Compute"
            iconName="calculator"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: (props.ui.layerOp === "calculate") && !props.buttonsDisabled })}
            onClick={() => props.onUiToggle("calculate")}
            disabled={props.buttonsDisabled}
          />

          <Button
            text="Geofence"
            iconName="polygon-filter"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: (props.geofencePaneVisible) && !props.buttonsDisabled })}
            onClick={props.onGeofencePaneToggle}
            disabled={props.buttonsDisabled}
          />

          <Button
            text="Chart"
            iconName="chart"
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: props.ui.panels.chart && !props.buttonsDisabled })}
            onClick={() => props.onUiToggle("chart")}
            disabled={props.buttonsDisabled}
          />

          <span className="pt-navbar-divider"></span>

          <div className={Classes.BUTTON_GROUP}>
            <ThemePicker
              selected={props.ui.settings.theme}
              onSelect={props.onSetTheme}
            />
            <Info />
          </div>
        </div>
      </nav>
    </div>
  );
}

const Info = () => (
  <Popover
    content={
      <div>
        <h5>Quartic Map</h5>
        <p>
          <b>Version:</b> {process.env.BUILD_VERSION || "unknown"}
        </p>
      </div>
    }
    popoverClassName="pt-popover-content-sizing"
    position={Position.BOTTOM}
  >
    <Button
      className={Classes.MINIMAL}
      iconName="info-sign"
    />
  </Popover>
);

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
      position={Position.BOTTOM}
    >
      <Button
        className={Classes.MINIMAL}
        iconName="settings"
      />
    </Popover>
  );
};

export default Toolbar;
