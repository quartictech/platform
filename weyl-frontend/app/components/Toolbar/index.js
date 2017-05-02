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
  const panes = [
    {
      name: "Layers",
      iconName: "layers",
      selected: props.ui.panels.layerList,
      onClick: () => props.onUiToggle("layerList"),
    },
    {
      name: "Compute",
      iconName: "calculator",
      selected: props.ui.layerOp === "calculate",
      onClick: () => props.onUiToggle("calculate"),
    },
    {
      name: "Geofence",
      iconName: "polygon-filter",
      selected: props.geofencePaneVisible,
      onClick: props.onGeofencePaneToggle,
    },
    {
      name: "Chart",
      iconName: "chart",
      selected: props.ui.panels.chart,
      onClick: () => props.onUiToggle("chart"),
    },
  ];

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
            layerList={props.layerList}
            onSelectLayer={props.onSelectLayer}
          />

          <span className="pt-navbar-divider"></span>
          
          {
            panes.map(p =>
              <PaneControl
                key={p.name}
                name={p.name}
                iconName={p.iconName}
                selected={p.selected}
                disabled={props.buttonsDisabled}
                onClick={p.onClick}
              />
            )
          }

          <span className="pt-navbar-divider"></span>
          <ThemePicker
            selected={props.ui.settings.theme}
            onSelect={props.onSetTheme}
          />
        </div>
      </nav>
    </div>
  );
}

const PaneControl = ({
  name,
  iconName,
  selected,
  disabled,
  onClick,
}) => (
  <Tooltip content={name} position={Position.BOTTOM}>
    <AnchorButton
      iconName={iconName}
      className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: selected && !disabled })}
      onClick={onClick}
      disabled={disabled}
    />
  </Tooltip>
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
      <Tooltip content="Pick a theme" position={Position.BOTTOM}>
        <Button
          className={Classes.MINIMAL}
          iconName="settings"
        />
      </Tooltip>
    </Popover>
  );
};

export default Toolbar;
