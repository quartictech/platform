import React from "react";
import styles from "./styles.css";

import BucketLayerItem from "../BucketLayerItem";
import GeofenceSettings from "../GeofenceSettings";
import LayerStyleSettings from "../LayerStyleSettings";
import AttributeList from "./AttributeList";

const DEFAULT_ICON = "grey map";

const LayerListItemInfo = ({
  layer,
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
              layerId={layer.id}
              attributes={layer.attributeSchema.attributes}
              filter={layer.filter}
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
              layerId={layer.id}
              layerAttributes={layer.attributeSchema.attributes}
              layerStyle={layer.style}
              onChange={onLayerStyleChange}
            />
          </div>
        </div>
      );


    case "INFO":
      return (
        <div className="ui secondary segment">
          <div className="content" style={{ "font-size": "0.8em" }}>
            {(layer.metadata.description)}
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
  <div className="item" style={{ paddingTop: "10px", paddingBottom: "10px" }}>
    <div className="content">
      <div className="right floated">
        <i className={`circular ${layer.metadata.icon || DEFAULT_ICON} icon`}></i>
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
    } else if (this.props.ui.layerOp === "geofence") {
      rows.push(<GeofenceSettings
        key="geofence"
        geofence={this.props.geofence}
        onGeofenceEdit={this.props.onGeofenceEdit}
        onGeofenceSave={this.props.onGeofenceSave}
        onGeofenceChangeType={this.props.onGeofenceChangeType}
      />);
    }
    for (const layer of this.props.layers) {
      rows.push(
        <LayerListItem
          key={layer.id}
          layer={layer}
          onButtonClick={(name) => this.onButtonClick(name, layer.id)}
          onToggleValueVisible={this.props.onToggleValueVisible}
          onLayerStyleChange={this.props.onLayerStyleChange}
          mode={(this.state.activeLayerId === layer.id) ? this.state.activeMode : null}
        />
      );
    }

    if (this.props.layers.length > 0 || this.props.ui.layerOp != null) {
      return (
        <div className={styles.layerList} style={{ "visibility": this.props.visible ? "visible" : "hidden" }}>
          <div className={styles.innerLayerList}>
            <div className="ui raised compact fluid card">
              <div className="content">
                <div className="ui divided items">
                  {rows}
                </div>
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
