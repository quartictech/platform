import React from "react";
import styles from "./styles.css";
import { layerThemes } from "../../themes";

import AttributeList from "./AttributeList";
import LayerStyleSettings from "../LayerStyleSettings";

const DEFAULT_ICON = "map";

const LayerListItem = ({
  layer,
  onButtonClick,
  onToggleValueVisible,
  onLayerStyleChange,
  onLayerThemeChange,
  mode,
}) => (
  <div className="item">
    <div className="content">
      <div className="right floated">
        <ThemePicker
          icon={layer.metadata.icon}
          themeIdx={layer.themeIdx}
          onClick={onLayerThemeChange}
        />
      </div>
      <div className="ui small header">
        <a onClick={() => onButtonClick("CLOSE")}>
          <i className="icon close"></i>
        </a>
        {layer.metadata.name}
      </div>
      <div className="description" style={{ marginTop: "0.2em" }}>
        <a onClick={() => onButtonClick("VISIBLE")} className={(layer.visible) ? styles.enabled : ""}>
          <i className="icon eye"></i>
        </a>
        <a onClick={() => onButtonClick("FILTER")} className={filterButtonStyle(layer, mode)}>
          <i className="icon filter"></i>
        </a>
        <a onClick={() => onButtonClick("STYLE")} className={styleButtonStyle(layer, mode)}>
          <i className="icon paint brush"></i>
        </a>
        <a onClick={() => onButtonClick("INFO")} className={infoButtonStyle(layer, mode)}>
          <i className="icon info"></i>
        </a>

        <LayerListItemInfo
          layer={layer}
          onAttributeValueClick={(l, a, v) => onToggleValueVisible(l, a, v)}
          onLayerStyleChange={onLayerStyleChange}
          mode={mode}
        />
      </div>
    </div>
  </div>
);

const ThemePicker = ({
  icon,
  themeIdx,
  onClick,
}) => (
  <a className="ui pointing left dropdown" ref={x => $(x).dropdown()}>
    <i className={`circular ${icon || DEFAULT_ICON} icon`} style={styleFromTheme(layerThemes[themeIdx])}></i>
    <div className="ui icon menu">
      {
        layerThemes.map((theme, idx) =>
          <div key={theme.name} className="item" onClick={() => onClick(idx)}>
            <i className="circular map icon" style={styleFromTheme(theme)}></i>
          </div>
        )
      }
    </div>
  </a>
);

const LayerListItemInfo = ({
  layer,
  onAttributeValueClick,
  onLayerStyleChange,
  mode,
}) => {
  switch (mode) {
    case "FILTER":
      return (
        <AttributeList
          layerId={layer.id}
          attributes={layer.attributeSchema.attributes}
          filter={layer.filter}
          onClick={onAttributeValueClick}
        />
      );

    case "STYLE":
      return (
        <LayerStyleSettings
          layerId={layer.id}
          layerAttributes={layer.attributeSchema.attributes}
          layerStyle={layer.style}
          onChange={onLayerStyleChange}
        />
      );


    case "INFO":
      return (
        <table className="ui celled very compact small fixed selectable definition table">
          <tbody>
            <tr><td>Description</td><td>{layer.metadata.description}</td></tr>
            {
              layer.metadata.attribution && (<tr><td>Attribution</td><td>{layer.metadata.attribution}</td></tr>)
            }
          </tbody>
        </table>
      );

    default:
      return null;
  }
};

const filterButtonStyle = (layer, mode) => {
  if (mode === "FILTER") {
    return styles.active;
  }
  if (Object.keys(layer.filter).some(k => layer.filter[k].length > 0)) {
    return styles.enabled;
  }
  return "";
};

const styleButtonStyle = (layer, mode) => (
  (mode === "STYLE") ? styles.active : ""
);

const infoButtonStyle = (layer, mode) => (
  (mode === "INFO") ? styles.active : ""
);

const styleFromTheme = (theme) => ({
  color: theme.line,
  backgroundColor: theme.fill,
});


export default LayerListItem;
