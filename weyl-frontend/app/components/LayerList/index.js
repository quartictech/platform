/**
*
* LayerList
*
*/

import React from 'react';
import naturalsort from 'javascript-natural-sort';

import styles from './styles.css';

import BucketLayerItem from "../BucketLayerItem";
import LayerStyleSettings from "../LayerStyleSettings";

const $ = require("jquery");

const AttributeValue = ({
  value,
  checked,
  onClick,
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
  onClick,
}) => (
  <div className="ui list">
    {values.sort(naturalsort).map(value => (
      <AttributeValue
        key={value}
        value={value}
        checked={!uncheckedValues.some(x => x === value)}
        onClick={(v) => onClick(attribute, v)}
      />
    ))}
  </div>
);

const AttributeList = ({
  layerId,
  attributes,
  filter,
  onClick,
}) => (
  <div className="content">
    {
      Object.keys(attributes)
        .filter(key => attributes[key].categories !== null)
        .sort(naturalsort)
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
                onClick={(a, v) => onClick(layerId, a, v)}
              />
            </div>
          </div>
        ))
    }
  </div>
);

const LayerListItemInfo = ({
  layerId,
  layerMetadata,
  attributes,
  filter,
  layerStyle,
  onAttributeValueClick,
  onLayerStyleChange,
  mode,
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
      return (
        <div className="ui secondary segment">
          <div className="content">
            <LayerStyleSettings
              layerId={layerId}
              layerAttributes={attributes}
              layerStyle={layerStyle}
              onChange={onLayerStyleChange}
            />
          </div>
        </div>
      );

    case "INFO":
      return (
        <div className="ui secondary segment">
          <div className="content">
            <table className="ui very basic celled very compact fixed selectable table">
              <tbody>
                <tr>
                  <td className="right aligned">
                    <div className="ui sub header">Attribution</div>
                  </td>
                  <td>{layerMetadata.attribution}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
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

const LayerListItem = ({
  layer,
  onButtonClick,
  onToggleValueVisible,
  onLayerStyleChange,
  mode,
}) => (
  <div className={styles.layerListItem}>
    <div className="content">
      <div className="right floated">
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
      </div>
      <div className="header">
        <a onClick={() => onButtonClick("CLOSE")}>
          <i className="icon close"></i>
        </a>
        {layer.metadata.name}
      </div>
      <div className="meta">
        {layer.metadata.description}
      </div>
    </div>

    <LayerListItemInfo
      layerId={layer.id}
      layerMetadata={layer.metadata}
      attributes={layer.attributeSchema.attributes}
      layerStyle={layer.style}
      filter={layer.filter}
      onAttributeValueClick={(l, a, v) => onToggleValueVisible(l, a, v)}
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
      activeMode: null,
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
          activeMode: (this.state.activeLayerId === layerId && this.state.activeMode === name) ? null : name,
        });
    }
  }

  render() {
    const rows = [];
    if (this.props.ui.layerOp === "bucket") {
      rows.push(<BucketLayerItem onCompute={this.props.onBucketCompute} layers={this.props.layers} onUiToggle={this.props.onUiToggle} key="bucket" />);
    }
    for (const layer of this.props.layers) {
      if (!layer.closed) {
        const key = `layer_${layer.id}`;
        rows.push(
          <LayerListItem
            key={key}
            layer={layer}
            onButtonClick={(name) => this.onButtonClick(name, layer.id)}
            onToggleValueVisible={this.props.onToggleValueVisible}
            onLayerStyleChange={this.props.onLayerStyleChange}
            mode={(this.state.activeLayerId === layer.id) ? this.state.activeMode : null}
          />
        );
      }
    }

    if (this.props.layers.filter(layer => !layer.closed).length > 0) {
      return (
        <div className={styles.layerList} style={{ "visibility": this.props.visible ? "visible" : "hidden" }}>
          <div className={styles.innerLayerList}>
            <div className="ui raised compact fluid card">
              <div className="content">
                <div className="header">
                </div>
                {rows}
              </div>
            </div>
          </div>
        </div>
      );
    }
    return null;
  }
}

export default LayerList;
