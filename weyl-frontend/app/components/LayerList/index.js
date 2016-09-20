/**
*
* LayerList
*
*/

import React from "react";

import { FormattedMessage } from "react-intl";
import messages from "./messages";
import styles from "./styles.css";
import classNames from "classnames";

import BucketLayerItem from "../BucketLayerItem";
import LayerStyleSettings from "../LayerStyleSettings";

var $ = require("jquery");


const AttributeValue = ({
  value,
  checked,
  onClick
}) => (
  <div className="item" key={value}>
    <div className="ui checked checkbox">
      <input type="checkbox" checked={checked} name={value} onChange={() => onClick(value)} />
      <label>{value}</label>
    </div>
  </div>
);

const AttributeValueList = ({
  attribute,
  values,
  uncheckedValues,
  onClick
}) => (
  <div className="ui list">
    {values.map(v => (
      <AttributeValue
        key={v}
        value={v}
        checked={!uncheckedValues.some(x => x === v)}
        onClick={(v) => onClick(attribute, v)}
      />
    ))}
  </div>
);

const AttributeList = ({
  layerId,
  attributes,
  filter,
  onClick
}) => (
  <div className="content">
    {
      Object.keys(attributes)
        .filter(key => attributes[key].categories !== null)
        .map(key => (
          <div className="ui accordion" key={key} ref={x => $(x).accordion()}>
            <div className="title">
              <i className="dropdown icon"></i>
              {key}
            </div>
            <div className="content">
              <AttributeValueList
                key={key}
                attribute={key}
                values={attributes[key].categories}
                uncheckedValues={filter.hasOwnProperty(key) ? filter[key] : []}
                onClick={(a,v) => onClick(layerId, a, v)}
              />
            </div>
          </div>
        ))
    }
  </div>
);

const LayerListItemInfo = ({
  layerId,
  attributes,
  filter,
  layerStyle,
  onAttributeValueClick,
  onLayerStyleChange,
  mode
}) => {
  switch (mode) {
    case "FILTER":
      return (
        <div className="ui secondary segment">
          <div className="content">
            <AttributeList
              layerId={layerId}
              attributes={attributes}
              filter={filter}
              onClick={onAttributeValueClick}
            />
          </div>
        </div>
      );

    case "STYLE":
    return (<div className="ui secondary segment">
          <div className="content">
          <LayerStyleSettings
            layerId = {layerId}
            layerAttributes = {attributes}
            layerStyle = {layerStyle}
            onChange = {onLayerStyleChange} />
          </div>
        </div>);
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
}

const styleButtonStyle = (layer, mode) => {
  return (mode === "STYLE") ? styles.active : "";
}

const LayerListItem = ({
  layer,
  onButtonClick,
  onToggleValueVisible,
  onLayerStyleChange,
  mode
}) => (
  <div className={styles.layerListItem}>
    <div className="content">
      <div className="right floated">
        <a onClick={e => onButtonClick("VISIBLE")} className={(layer.visible) ? styles.enabled : ""}>
          <i className="icon eye"></i>
        </a>
        <a onClick={e => onButtonClick("FILTER")} className={filterButtonStyle(layer, mode)}>
          <i className="icon filter"></i>
        </a>
        <a onClick={e => onButtonClick("STYLE")} className={styleButtonStyle(layer, mode)}>
          <i className="icon paint brush"></i>
        </a>
      </div>
      <div className="header">
        <a onClick={e => onButtonClick("CLOSE")}>
          <i className="icon close"></i>
        </a>
        {layer.name}
      </div>
      <div className="meta">
        {layer.description}
      </div>
    </div>

    <LayerListItemInfo
      layerId={layer.id}
      attributes={layer.attributeSchema.attributes}
      layerStyle={layer.style}
      filter={layer.filter}
      onAttributeValueClick={(l,a,v) => onToggleValueVisible(l,a,v)}
      onLayerStyleChange={onLayerStyleChange}
      mode={mode}
    />
  </div>
);

class LayerList extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      activeLayerId: null,
      activeMode: null
    };
  }

  onButtonClick(name, layerId) {
    switch (name) {
      case "VISIBLE":
        return this.props.layerToggleVisible(layerId);
      case "CLOSE":
        return this.props.layerClose(layerId);
      default:
        return this.setState({
          activeLayerId: layerId,
          activeMode: (this.state.activeLayerId === layerId && this.state.activeMode === name) ? null : name
        });
    }
  }

  render() {
    let rows = [];
    if (this.props.ui.layerOp == "bucket") {
      rows.push(<BucketLayerItem onCompute={this.props.onBucketCompute} layers={this.props.layers} onUiToggle={this.props.onUiToggle} key="bucket"/>)
    }
    for (const layer of this.props.layers) {
      if (layer.closed) {
        continue;
      }
      let key="layer_" + layer.id;
      rows.push(<LayerListItem
        key={key}
        layer={layer}
        onButtonClick={(name) => this.onButtonClick(name, layer.id)}
        onToggleValueVisible={this.props.onToggleValueVisible}
        onLayerStyleChange={this.props.onLayerStyleChange}
        mode={(this.state.activeLayerId === layer.id) ? this.state.activeMode : null}
        />);
    }

    if (this.props.layers.length > 0) {
      return (
        <div className={styles.layerList} style={{"visibility": this.props.visible ? "visible" : "hidden"}}>
          <div className="ui raised fluid card">
            <div className={styles.innerLayerList}>
              {rows}
            </div>
          </div>
        </div>
      );
    }
    else {
      return null;
    }
  }
}

export default LayerList;
