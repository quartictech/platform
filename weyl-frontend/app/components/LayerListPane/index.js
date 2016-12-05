import React from "react";
import {
  Button,
  Classes,
  Intent,
  Tree,
} from "@blueprintjs/core";
import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
import * as _ from "underscore";

import LayerListItem from "./LayerListItem";
import Pane from "../Pane";


class LayerListPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      activeLayerId: null,
      activeMode: null,
      nodes: [],
    };
    this.onNodeExpand = this.onNodeExpand.bind(this);
    this.onNodeCollapse = this.onNodeCollapse.bind(this);
    this.onNodeClick = this.onNodeClick.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    // Rely on immutables shallow equality
    if (nextProps.layers !== this.props.layers) {
      this.setState({ nodes: this.layerNodes(nextProps.layers.toJS()) });
    }
  }

  render() {
    return (
      <Pane
        title="Layers"
        iconName="layers"
        visible={this.props.visible && _.size(this.props.layers.toJS()) > 0}
        onClose={this.props.onClose}
      >
        <Tree
          contents={this.state.nodes}
          onNodeExpand={this.onNodeExpand}
          onNodeCollapse={this.onNodeCollapse}
          onNodeClick={this.onNodeClick}
        />
      </Pane>
    );
  }

  onNodeExpand(node) {
    const siblings = (node.parent) ? node.parent.childNodes : this.state.nodes;
    siblings.forEach(n => this.collapseNodeAndChildren(n));
    node.isExpanded = true;
    this.triggerRender();
  }

  onNodeCollapse(node) {
    this.collapseNodeAndChildren(node);
    this.triggerRender();
  }

  collapseNodeAndChildren(node) {
    node.isExpanded = false;
    if (node.childNodes) {
      node.childNodes.forEach(n => this.collapseNodeAndChildren(n));
    }
  }

  onNodeClick(node) {
    if (node.isExpanded) {
      this.onNodeCollapse(node);
    } else {
      this.onNodeExpand(node);
    }
  }

  triggerRender() {
    this.setState(this.state);
  }

  layerNodes(layers) {
    return _.map(_.values(layers), layer => {
      const node = {
        iconName: "layer",
        id: layer.id,
        label: layer.metadata.name,
        secondaryLabel: this.buttons(
          layer,
          (this.state.activeLayerId === layer.id) ? this.state.activeMode : null,
          (name) => this.onButtonClick(name, layer.id)
        ),
      };
      node.childNodes = this.attributeNodes(layer.attributeSchema.attributes, node);
      return node;
    });
  }

  attributeNodes(attributes, parent) {
    return _.chain(attributes)
      .keys()
      .filter(k => attributes[k].categories !== null)
      .sort(naturalsort)
      .map(k => {
        const node = {
          iconName: "property",
          id: k,
          label: k,
          parent,
        };
        node.childNodes = this.attributeCategoryNodes(attributes[k].categories, node);
        return node;
      })
      .value();
  }

  attributeCategoryNodes(categories, parent) {
    return _.chain(categories)
      .sort(naturalsort)
      .map(c => ({
        id: c,
        label: c,
        secondaryLabel: (
          <Button
            iconName={true ? "eye-open" : "eye-off"}
            onClick={() => onClick("VISIBLE")}
            className={Classes.MINIMAL}
            intent={true ? Intent.SUCCESS : Intent.NONE}
          />
        ),
        parent,
      }))
      .value();
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

  // TODO: wire this up to the top-level eye intent
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



export default LayerListPane;
