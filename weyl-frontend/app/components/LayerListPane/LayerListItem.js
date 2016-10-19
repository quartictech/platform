import React from "react";
import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
import styles from "./styles.css";
import { layerThemes } from "../../themes";

import AttributeList from "./AttributeList";
const $ = require("jquery");
const _ = require("underscore");

const DEFAULT_ICON = "map";

class LayerBufferMenu extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = { bufferDistance: 100 };
  }

  onBufferClick() {
    $(".ui.dropdown").dropdown("hide");
    this.props.onBufferClick(this.state.bufferDistance);
  }

  render() {
    return (
      <div className="item">
        <i className="dropdown icon"></i>
        <span className="text">Buffer</span>
        <div className="ui menu">
          <div className="ui action labeled input">
            <div className="ui label">
              m
            </div>
            <input
              placeholder="Buffer Distance..."
              type="number"
              id="buffer-distance-input"
              value={this.state.bufferDistance}
              onChange={(e) => this.setState({ bufferDistance: e.target.value })}
            />
            <button className="ui compact icon button" onClick={() => this.onBufferClick()}>
              <i className="right arrow icon" style={{ paddingTop: 0, paddingBottom: 0 }}></i>
            </button>
          </div>
        </div>
      </div>
    );
  }
}

const LayerListItem = ({
  layer,
  onButtonClick,
  onToggleValueVisible,
  onLayerStyleChange,
  onLayerThemeChange,
  onBufferClick,
  mode,
}) => (
  <div className="item">
    <div className="content">
      <div className="right floated">
        <ThemePicker
          icon={layer.metadata.icon || DEFAULT_ICON}
          themeIdx={layer.themeIdx}
          attributes={layer.attributeSchema.attributes}
          selectedAttribute={layer.style.attribute}
          onThemeClick={onLayerThemeChange}
          onAttributeClick={onLayerStyleChange}
          onBufferClick={onBufferClick}
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
        <a onClick={() => onButtonClick("INFO")} className={infoButtonStyle(layer, mode)}>
          <i className="icon info"></i>
        </a>

        <LayerListItemInfo
          layer={layer}
          onAttributeValueClick={(l, a, v) => onToggleValueVisible(l, a, v)}
          mode={mode}
        />
      </div>
    </div>
  </div>
);

const ThemePicker = ({
  icon,
  themeIdx,
  attributes,
  selectedAttribute,
  onThemeClick,
  onAttributeClick,
  onBufferClick,
}) => (
  <a className="ui pointing left dropdown" ref={x => $(x).dropdown({ action: "hide" })}>
    <i className={`circular ${icon} icon`} style={styleFromTheme(layerThemes[themeIdx])}></i>
    <div className="ui menu">
      <div className="item">
        <i className="dropdown icon"></i>
        <span className="text">Theme</span>
        <div className="ui right icon menu">
          {
            layerThemes.map((theme, idx) =>
              <div key={theme.name} className="item" onClick={() => onThemeClick(idx)}>
                <i className={`circular ${icon} icon`} style={styleFromTheme(theme)}></i>
              </div>
            )
          }
        </div>
      </div>
      <div className="item">
        <i className="dropdown icon"></i>
        <span className="text">Colour by</span>
        <div className="ui right vertical menu">
          <div key="none" className={classNames("item", { active: selectedAttribute === null })} onClick={() => onAttributeClick(null)}>None</div>
          {
            _.keys(attributes)
              .filter(k => attributes[k].type === "NUMERIC")
              .sort(naturalsort)
              .map(k =>
                <div key={k} className={classNames("item", { active: selectedAttribute === k })} onClick={() => onAttributeClick(k)}>
                  {k}
                </div>
              )
          }
        </div>
      </div>
      <LayerBufferMenu onBufferClick={onBufferClick} />

    </div>
  </a>
);

const LayerListItemInfo = ({
  layer,
  onAttributeValueClick,
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

const infoButtonStyle = (layer, mode) => (
  (mode === "INFO") ? styles.active : ""
);

const styleFromTheme = (theme) => ({
  color: theme.line,
  backgroundColor: theme.fill,
});


export default LayerListItem;
