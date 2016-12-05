import React from "react";
import {
  Button,
  Classes,
  Intent,
  Tree,
} from "@blueprintjs/core";
import classNames from "classnames";
import * as _ from "underscore";

import LayerListItem from "./LayerListItem";
import Pane from "../Pane";


class LayerListPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      activeLayerId: null,
      activeMode: null,
    };
    this.nodes = this.nodes.bind(this);
    this.buttons = this.buttons.bind(this);
    this.filterActive = this.filterActive.bind(this);
  }

  render() {
    return (
      <Pane
        title="Layers"
        iconName="layers"
        visible={this.props.visible && _.size(this.props.layers.toArray()) > 0}
        onClose={this.props.onClose}
      >
        <Tree
          contents={this.nodes()}
        />
      </Pane>
    );
  }

  nodes() {
    return _.map(this.props.layers.toArray(), layer => ({
      iconName: "layer",
      id: layer.get("id"),
      label: layer.getIn(["metadata", "name"]),
      secondaryLabel: this.buttons(
        layer.toJS(),
        (this.state.activeLayerId === layer.get("id")) ? this.state.activeMode : null,
        (name) => this.onButtonClick(name, layer.get("id"))
      ),
    }));

    //   <LayerListItem
    //     key={layer.get("id")}
    //     layer={layer.toJS()}
    //     onButtonClick={(name) => this.onButtonClick(name, layer.get("id"))}
    //     onToggleValueVisible={this.props.onToggleValueVisible}
    //     onLayerStyleChange={(attribute) => this.props.onLayerStyleChange(layer.get("id"), "ATTRIBUTE", attribute)}
    //     onLayerThemeChange={(idx) => this.props.onLayerStyleChange(layer.get("id"), "THEME", idx)}
    //     onBufferClick={(bufferDistance) => this.onBufferCompute(layer.get("id"), bufferDistance)}
    //     mode={(this.state.activeLayerId === layer.get("id")) ? this.state.activeMode : null}
    //   />
    // );
  }

  buttons(layer, mode, onClick) {
    return (
      <div className={Classes.BUTTON_GROUP}>
        <Button
          iconName={layer.visible ? "eye-open" : "eye-off"}
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
          intent={this.filterActive(layer) ? Intent.DANGER : Intent.NONE}
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
          iconName="cross"
          onClick={() => onClick("CLOSE")}
          className={Classes.MINIMAL}

        />
      </div>
    );
  }

  filterActive(layer) {
    return _.some(layer.filter, attr => (_.size(attr.categories) > 0) || attr.notApplicable);
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

  onBufferCompute(layerId, bufferDistance) {
    this.props.onCompute({
      type: "buffer",
      layerId,
      bufferDistance,
    });
  }
}

export default LayerListPane;
