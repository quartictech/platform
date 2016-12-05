import React from "react";
import {
  Button,
  Classes,
  Collapse,
  Intent,
} from "@blueprintjs/core";
import classNames from "classnames";
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
  <div className={Classes.CARD} style={{ padding: 0 }}>
    <h6>
      <Button
        iconName="caret-down"
        className={Classes.MINIMAL}
      />
      {layer.metadata.name}
    </h6>

    <Collapse isOpen={false}>
      <Buttons
        layer={layer}
        mode={mode}
        onClick={onButtonClick}
      />

      <div className="description" style={{ marginTop: "0.2em" }}>
        <LayerListItemInfo
          layer={layer}
          onAttributeValueClick={(l, a, v) => onToggleValueVisible(l, a, v)}
          mode={mode}
        />
      </div>
    </Collapse>
  </div>
);


// <ThemePicker
//   icon={layer.metadata.icon || DEFAULT_ICON}
//   themeIdx={layer.themeIdx}
//   attributes={layer.attributeSchema.attributes}
//   selectedAttribute={layer.style.attribute}
//   onThemeClick={onLayerThemeChange}
//   onAttributeClick={onLayerStyleChange}
//   onBufferClick={onBufferClick}
// />


const Buttons = ({
  layer,
  mode,
  onClick,
}) => (
  <div className={Classes.BUTTON_GROUP}>
    <Button
      iconName="eye-open"
      onClick={() => onClick("VISIBLE")}
      className={Classes.MINIMAL}
      intent={layer.visible ? Intent.SUCCESS : Intent.NONE}
    />
    <Button
      iconName="filter"
      onClick={() => onClick("FILTER")}
      className={classNames(
        Classes.MINIMAL,
        { [Classes.ACTIVE]: (mode === "FILTER") }
      )}
      intent={filterActive(layer) ? Intent.DANGER : Intent.NONE}
    />
    <Button
      iconName="info-sign"
      onClick={() => onClick("INFO")}
      className={classNames(
        Classes.MINIMAL,
        { [Classes.ACTIVE]: (mode === "INFO") }
      )}
    />
    <Button
      iconName="small-cross"
      onClick={() => onClick("CLOSE")}
      className={Classes.MINIMAL}

    />
  </div>
);

const filterActive = (layer) =>
  _.some(layer.filter, attr => (_.size(attr.categories) > 0) || attr.notApplicable);

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
        <table className="pt-table pt-striped pt-condensed pt-interactive" style={{ width: "100%" }}>
          <tbody>
            <tr><td><b>Description</b></td><td>{layer.metadata.description}</td></tr>
            <tr><td><b>Attribution</b></td><td>{layer.metadata.attribution}</td></tr>
          </tbody>
        </table>
      );

    default:
      return null;
  }
};


export default LayerListItem;
