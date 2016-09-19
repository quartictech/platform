/**
*
* LayerStyleSettings
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

import LayerAttributePicker from '../LayerAttributePicker';

class LayerStyleSettings extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.radioButtons = {};
    this.state = { attribute: null };
  }

  renderRadioButton(name, state) {
        return <input name={this.props.layerId} id={name} checked={state} type="radio" onChange={this.onRadioChange.bind(this)}
          ref={x => this.radioButtons[name] = x}/>
  }

  onRadioChange(e) {
    if (e.currentTarget.id == "choropleth") {
      this.state.attribute = this.getNumericAttributes()[0];
    }

    let property = (e.currentTarget.id == "constant_fill") ? null : this.state.attribute;
    this.props.onChange(this.props.layerId, {
        polygon: {
           property
        }
    });
  }

  onAttributeChange(value) {
    this.state.attribute = value;

    this.props.onChange(this.props.layerId, {
      polygon: {
        property: this.radioButtons["constant_fill"].checked ? null : this.state.attribute
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
        <LayerAttributePicker attributes={numericAttributes} selected={this.props.layerStyle.polygon.property} onChange={this.onAttributeChange.bind(this)}/>
      </div>
    </div>
  </div>
    );
  }
}

export default LayerStyleSettings;
