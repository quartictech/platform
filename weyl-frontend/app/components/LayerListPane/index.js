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
      activeAttribute: null,
      nodes: [],
    };
  }

  componentWillReceiveProps(nextProps) {
    // Rely on immutables shallow equality
    if (nextProps.layers !== this.props.layers) {
      this.state.nodes = this.layerNodes(nextProps.layers.toJS());
      this.updateExpansion(this.state.nodes);
      this.setState(this.state);
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
          onNodeExpand={n => n.onExpand(n)}
          onNodeCollapse={n => n.onCollapse(n)}
          onNodeClick={n => n.onClick(n)}
        />
      </Pane>
    );
  }

  setActivePath(activeLayerId, activeAttribute) {
    this.state.activeLayerId = activeLayerId;
    this.state.activeAttribute = activeAttribute;
    this.updateExpansion(this.state.nodes);
    this.setState(this.state);
  }

  updateExpansion(nodes) {
    if (!nodes) {
      return;
    }
    nodes.forEach(node => {
      node.updateExpansion(node);
      this.updateExpansion(node.childNodes);
    });
  }

  layerNodes(layers) {
    return _.map(_.values(layers), layer => ({
      iconName: "layer",
      id: layer.id,
      label: layer.metadata.name,
      secondaryLabel: this.buttons(
        layer,
        (name) => this.onButtonClick(name, layer.id)
      ),
      childNodes: this.attributeNodes(
        layer.attributeSchema.attributes,
        layer.filter,
        (k, v) => this.props.onToggleValueVisible(layer.id, k, v),
      ),
      onClick: (n) => ((this.state.activeLayerId === layer.id) ? n.onCollapse(n) : n.onExpand(n)),
      onExpand: (n) => this.setActivePath(layer.id, null),
      onCollapse: (n) => this.setActivePath(null, null),
      updateExpansion: (n) => (n.isExpanded = (this.state.activeLayerId === layer.id)),
    }));
  }

  attributeNodes(attributes, filter, onValueClick) {
    return _.chain(attributes)
      .keys()
      .filter(k => attributes[k].categories !== null)
      .sort(naturalsort)
      .map(k => ({
        iconName: "property",
        id: k,
        label: k,
        childNodes: this.attributeCategoryNodes(
          attributes[k].categories,
          filter[k].categories,
          (v) => onValueClick(k, v),
        ),
        onClick: (n) => ((this.state.activeAttribute === k) ? n.onCollapse(n) : n.onExpand(n)),
        onExpand: (n) => this.setActivePath(this.state.activeLayerId, k),
        onCollapse: (n) => this.setActivePath(this.state.activeLayerId, null),
        updateExpansion: (n) => (n.isExpanded = (this.state.activeAttribute === k)),
      }))
      .value();
  }

  attributeCategoryNodes(categories, filterCategories, onClick) {
    return _.chain(categories)
      .sort(naturalsort)
      .map(c => ({
        id: c,
        label: c,
        secondaryLabel: (
          <Button
            iconName={!_.contains(filterCategories, c) ? "eye-open" : "eye-off"}
            onClick={withNoPropagation(() => onClick(c))}
            className={Classes.MINIMAL}
            intent={!_.contains(filterCategories, c) ? Intent.PRIMARY : Intent.NONE}
          />
        ),
        onClick: () => onClick(c),
        onExpand: () => {},
        onCollapse: () => {},
        updateExpansion: () => {},
      }))
      .value();
  }

  buttons(layer, onClick) {
    return (
      <div className={Classes.BUTTON_GROUP}>
        <Button
          iconName={layer.visible ? "eye-open" : "eye-off"}
          onClick={withNoPropagation(() => onClick("VISIBLE"))}
          className={Classes.MINIMAL}
          intent={layer.visible ? (this.filterActive(layer) ? Intent.WARNING : Intent.PRIMARY) : Intent.NONE}
        />
        <Button
          iconName="info-sign"
          onClick={withNoPropagation(() => onClick("INFO"))}
          className={Classes.MINIMAL}
        />
        <Button
          iconName="cross"
          onClick={withNoPropagation(() => onClick("CLOSE"))}
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

const withNoPropagation = (func) => (e) => {
  e.stopPropagation();
  func();
};

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
