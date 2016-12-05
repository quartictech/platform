import React from "react";
import {
  Button,
  Classes,
  Intent,
} from "@blueprintjs/core";
import classNames from "classnames";
import styles from "./styles.css";
import ThemePicker from "./ThemePicker";
import AttributeList from "./AttributeList";
const _ = require("underscore");

const DEFAULT_ICON = "map";

const LayerListItem = ({
  layer,
  onButtonClick,
  onToggleValueVisible,
  onLayerStyleChange,
  onLayerThemeChange,
  onBufferClick,
  mode,
}) => (
  <tr>
    <td>
      <div className="ui small header">
        <a onClick={() => onButtonClick("CLOSE")}>
          <i className="icon close"></i>
        </a>
        {layer.metadata.name}
      </div>

      <div className={Classes.BUTTON_GROUP}>
        <Button
          iconName="eye-open"
          onClick={() => onButtonClick("VISIBLE")}
          className={Classes.MINIMAL}
          intent={layer.visible ? Intent.SUCCESS : Intent.NONE}
        />
        <Button
          iconName="filter"
          onClick={() => onButtonClick("FILTER")}
          className={classNames(
            Classes.MINIMAL,
            { [Classes.ACTIVE]: (mode === "FILTER") }
          )}
          intent={filterActive(layer) ? Intent.DANGER : Intent.NONE}
        />
        <Button
          iconName="info-sign"
          onClick={() => onButtonClick("INFO")}
          className={classNames(
            Classes.MINIMAL,
            { [Classes.ACTIVE]: (mode === "INFO") }
          )}
        />
      </div>


      <div className="description" style={{ marginTop: "0.2em" }}>

        <LayerListItemInfo
          layer={layer}
          onAttributeValueClick={(l, a, v) => onToggleValueVisible(l, a, v)}
          mode={mode}
        />
      </div>
    </td>
    <td>
      <ThemePicker
        icon={layer.metadata.icon || DEFAULT_ICON}
        themeIdx={layer.themeIdx}
        attributes={layer.attributeSchema.attributes}
        selectedAttribute={layer.style.attribute}
        onThemeClick={onLayerThemeChange}
        onAttributeClick={onLayerStyleChange}
        onBufferClick={onBufferClick}
      />
    </td>
  </tr>
);

const filterActive = (layer) => _.some(layer.filter, attr => (_.size(attr.categories) > 0) || attr.notApplicable);

const filterButtonStyle = (layer, mode) => {
  if (mode === "FILTER") {
    return styles.active;
  }
  if (Object.keys(layer.filter).some(k => layer.filter[k].length > 0)) {
    return styles.enabled;
  }
  return "";
};

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


export default LayerListItem;
