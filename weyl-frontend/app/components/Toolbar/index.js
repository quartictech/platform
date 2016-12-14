import React from "react";
import {
  Button,
  AnchorButton,
  Classes,
  IconContents,
  Menu,
  MenuItem,
  MenuDivider,
  Popover,
  Position,
  Tooltip,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";
import Search from "../Search";
import { mapThemes } from "../../themes";
import styles from "./styles.css";
import logo from "./quartic.svg";

function Toolbar(props) {
  return (
    <div className={classNames(styles.toolbar, "pt-elevation-3")}>
      <nav className="pt-navbar pt-dark">
        <div className="pt-navbar-group pt-align-left">
          <img
            src={logo}
            role="presentation"
            style={{ maxWidth: "70%", maxHeight: "70%" }}
          />

          <span className="pt-navbar-divider"></span>
          <Search
            onSearch={props.onSearch}
            onSelectLayer={props.onSelectLayer}
            onSelectPlace={props.onSelectPlace}
          />

          <span className="pt-navbar-divider"></span>
          <Tooltip content="Layers" position={Position.BOTTOM}>
            <AnchorButton
              iconName="layers"
              className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: props.ui.panels.layerList && !props.buttonsDisabled })}
              onClick={() => props.onUiToggle("layerList")}
              disabled={props.buttonsDisabled}
            />
          </Tooltip>

          <Tooltip content="Compute" position={Position.BOTTOM} inlin>
            <AnchorButton
              iconName="calculator"
              className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: (props.ui.layerOp === "calculate") && !props.buttonsDisabled })}
              onClick={() => props.onUiToggle("calculate")}
              disabled={props.buttonsDisabled}
            />
          </Tooltip>

          <Tooltip content="Geofence" position={Position.BOTTOM}>
            <AnchorButton
              iconName="polygon-filter"
              className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: (props.geofencePaneVisible) && !props.buttonsDisabled })}
              onClick={props.onGeofencePaneToggle}
              disabled={props.buttonsDisabled}
            />
          </Tooltip>

          <Tooltip content="Chart" position={Position.BOTTOM}>
            <AnchorButton
              iconName="chart"
              className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: props.ui.panels.chart && !props.buttonsDisabled })}
              onClick={() => props.onUiToggle("chart")}
              disabled={props.buttonsDisabled}
            />
          </Tooltip>

          <span className="pt-navbar-divider"></span>
          <Tooltip content="Pick a theme" position={Position.BOTTOM}>
            <ThemePicker
              selected={props.ui.settings.theme}
              onSelect={props.onSetTheme}
            />
          </Tooltip>
        </div>
      </nav>
    </div>
  );
}

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
        />)}
      <MenuDivider />
      <MenuItem
        key={"info"}
        text={`Quartic Map (version: ${process.env.BUILD_VERSION || "unknown"})`}
        iconName="info-sign"
        disabled
      />
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
