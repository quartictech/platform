/**
*
* LayerStyleSettings
*
*/

import React from "react";

import LayerAttributePicker from "../LayerAttributePicker";

class LayerStyleSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.radioButtons = {};
    this.onAttributeChange = this.onAttributeChange.bind(this);
    this.onRadioChange = this.onRadioChange.bind(this);
  }

  renderRadioButton(name, label, state) {
    return (
      <div className="ui radio checkbox">
        <input
          name={this.props.layerId}
          id={name}
          checked={state}
          type="radio"
          onChange={this.onRadioChange}
          ref={x => (this.radioButtons[name] = x)}
        />
        <label htmlFor={name}>{label}</label>
      </div>
    );
  }

  onRadioChange(e) {
    const property = (e.currentTarget.id === "constant_fill") ? null : this.getNumericAttributes()[0];

    this.props.onChange(this.props.layerId, {
      property,
    });
  }

  onAttributeChange(value) {
    this.props.onChange(this.props.layerId, {
      property: this.radioButtons.constant_fill.checked ? null : value,
    });
  }

  getNumericAttributes() {
    const attributeStats = this.props.layerAttributes;
    const numericAttributes = [];
    attributeStats.forEach(prop => {
      if (attributeStats[prop].type === "NUMERIC") {
        numericAttributes.push(prop);
      }
    });
    return numericAttributes;
  }

  renderAttributePicker(visible, numericAttributes) {
    if (visible) {
      return (
        <LayerAttributePicker
          attributes={numericAttributes}
          selected={this.props.layerStyle.property}
          onChange={this.onAttributeChange}
        />);
    }
    return null;
  }

  render() {
    const numericAttributes = this.getNumericAttributes();
    return (
      <div className="ui form">
        <div className="grouped fields">
          <div className="field">
            {this.renderRadioButton("constant_fill", "Constant Fill", this.props.layerStyle.property == null)}
          </div>
          <div className="field">
            {this.renderRadioButton("choropleth", "Color By", this.props.layerStyle.property != null)}
            {this.renderAttributePicker(this.props.layerStyle.property != null, numericAttributes)}
          </div>
        </div>
      </div>
    );
  }
}

export default LayerStyleSettings;
