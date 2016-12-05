import React from "react";
import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");

import { layerThemes } from "../../themes";

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

const styleFromTheme = (theme) => ({
  color: theme.line,
  backgroundColor: theme.fill,
});

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

export default ThemePicker;
