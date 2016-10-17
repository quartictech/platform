import React from "react";
import styles from "./styles.css";

import AttributeList from "./AttributeList";
import LayerStyleSettings from "../LayerStyleSettings";

const DEFAULT_ICON = "grey map";

const LayerListItem = ({
  layer,
  onButtonClick,
  onToggleValueVisible,
  onLayerStyleChange,
  mode,
}) => (
  <div className="item">
    <div className="content">
      <div className="right floated">
        <a className="ui pointing left dropdown" ref={x => $(x).dropdown()}>
          <i className={`circular ${layer.metadata.icon || DEFAULT_ICON} icon`}></i>
          <div className="ui icon menu">
            <div className="item"><i className="circular map icon" style={{ color: "#e7298a", backgroundColor: "#67001f" }}></i></div>
            <div className="item"><i className="circular map icon" style={{ color: "#29e78a", backgroundColor: "#00671f" }}></i></div>
            <div className="item"><i className="circular map icon" style={{ color: "#298ae7", backgroundColor: "#001f67" }}></i></div>
            <div className="item"><i className="circular map icon" style={{ color: "#8a29e7", backgroundColor: "#1f0067" }}></i></div>
          </div>
        </a>
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


export default LayerListItem;
