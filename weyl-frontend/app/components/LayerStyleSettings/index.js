/**
*
* LayerStyleSettings
*
*/

import React from "react";

import { FormattedMessage } from "react-intl";
import messages from "./messages";
import styles from "./styles.css";

import LayerAttributePicker from "../LayerAttributePicker";

class LayerStyleSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.radioButtons = {};
  }

  renderRadioButton(name, state) {
        return <input name={this.props.layerId} id={name} checked={state} type="radio" onChange={this.onRadioChange.bind(this)}
          ref={x => this.radioButtons[name] = x}/>
  }

  onRadioChange(e) {
    let property = (e.currentTarget.id == "constant_fill") ? null : this.getNumericAttributes()[0];

    this.props.onChange(this.props.layerId, {
        polygon: {
           property
        }
    });
  }

  onAttributeChange(value) {
    this.props.onChange(this.props.layerId, {
      polygon: {
        property: this.radioButtons["constant_fill"].checked ? null : value
      }
    })
  }

  getNumericAttributes() {
    let attributeStats = this.props.layerAttributes;
    let numericAttributes = [];
    for (var prop in attributeStats) {
      if (attributeStats[prop]["type"] === "NUMERIC") {
        numericAttributes.push(prop);
      }
    }

    return numericAttributes;
  }

  renderAttributePicker(visible, numericAttributes) {
    if (visible) {
      return (<LayerAttributePicker
        attributes={numericAttributes}
        selected={this.props.layerStyle.polygon.property}
        onChange={this.onAttributeChange.bind(this)}/>);
    }
    else {
      return null;
    }
  }

  render() {
    let numericAttributes = this.getNumericAttributes();
    return (
      <div className="ui form">
    <div className="grouped fields">
      <div className="field">
        <div className="ui radio checkbox">
          {this.renderRadioButton("constant_fill", this.props.layerStyle.polygon.property == null)}
          <label>Constant Fill</label>
        </div>
      </div>
      <div className="field">
        <div className="ui radio checkbox">
          {this.renderRadioButton("choropleth", this.props.layerStyle.polygon.property != null)}
          <label>Choropleth</label>
        </div>
        {this.renderAttributePicker(this.props.layerStyle.polygon.property != null, numericAttributes)}
      </div>
    </div>
  </div>
    );
  }
}

export default LayerStyleSettings;
