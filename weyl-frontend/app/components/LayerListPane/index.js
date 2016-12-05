import React from "react";
import {
  Button,
  Classes,
  Intent,
  Menu,
  MenuDivider,
  MenuItem,
  Popover,
  PopoverInteractionKind,
  PopoverPosition,
  Position,
  Tag,
  Tree,
} from "@blueprintjs/core";
import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
import * as _ from "underscore";

import { layerThemes } from "../../themes";
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
    // We maintain the state for the tree, which we have to keep in-sync with props
    if (nextProps.layers !== this.props.layers) {
      this.setState({ nodes: this.layerNodes(nextProps.layers.toJS()) });
    }
  }

  render() {
    // We delegate the tree callbacks to the individual nodes to get granular behaviour
    return (
      <Pane
        title="Layers"
        iconName="layers"
        visible={this.props.visible && _.size(this.props.layers.toJS()) > 0}
        onClose={this.props.onClose}
      >
        <Tree
          contents={this.state.nodes}
          onNodeExpand={n => n.onExpand()}
          onNodeCollapse={n => n.onCollapse()}
          onNodeClick={(n, p, e) => (e.target.nodeName !== "BUTTON") && n.onClick() } // Because the buttons propagate events
        />
      </Pane>
    );
  }

  setState(nextState) {
    this.state = { ...this.state, ...nextState };
    recursivelyUpdateExpansionState(this.state.nodes);  // Get the nodes to update their own isExpanded values
    super.setState(this.state);
  }

  layerNodes(layers) {
    return _.map(_.values(layers), layer => {
      const node = {
        // iconName: "layer",
        id: layer.id,
        label: (
          <div>
            <span
              className={classNames(Classes.iconClass("layers"), Classes.ICON_STANDARD, Classes.TREE_NODE_ICON)}
              style={styleFromTheme(layerThemes[layer.themeIdx])}
            />
            {layer.metadata.name}
          </div>
        ),
        secondaryLabel: this.layerButtons(layer),
        childNodes: this.attributeNodes(
          layer.attributeSchema.attributes,
          layer.filter,
          (k, v) => this.props.onToggleValueVisible(layer.id, k, v),
        ),
      };
      node.onClick = () => toggleOnPredicate(node, this.state.activeLayerId === layer.id);
      node.onExpand = () => this.setState({ activeLayerId: layer.id, activeAttribute: null });
      node.onCollapse = () => this.setState({ activeLayerId: null, activeAttribute: null });
      node.updateExpansion = () => (node.isExpanded = (this.state.activeLayerId === layer.id));
      return node;
    });
  }

  attributeNodes(attributes, filter, onValueClick) {
    return _.chain(attributes)
      .keys()
      .filter(k => attributes[k].categories)
      .sort(naturalsort)
      .map(k => {
        const node = {
          iconName: "property",
          id: k,
          label: k,
          childNodes: this.attributeCategoryNodes(
            attributes[k].categories,
            filter[k].categories,
            (v) => onValueClick(k, v),
          ),
        };
        node.onClick = () => toggleOnPredicate(node, this.state.activeAttribute === k);
        node.onExpand = () => this.setState({ activeAttribute: k });
        node.onCollapse = () => this.setState({ activeAttribute: null });
        node.updateExpansion = () => (node.isExpanded = (this.state.activeAttribute === k));
        return node;
      })
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
            onClick={() => onClick(c)}
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

  layerButtons(layer) {
    return (
      <div className={Classes.BUTTON_GROUP}>
        <Button
          iconName={layer.visible ? "eye-open" : "eye-off"}
          onClick={() => this.props.layerToggleVisible(layer.id)}
          className={Classes.MINIMAL}
          intent={layer.visible ? (this.filterActive(layer) ? Intent.WARNING : Intent.PRIMARY) : Intent.NONE}
        />
        <Popover
          autoFocus={false}
          enforceFocus={false}
          content={this.layerSettings(layer)}
          interactionKind={PopoverInteractionKind.CLICK}
          position={Position.RIGHT_TOP}
        >
          <Button
            iconName="settings"
            className={Classes.MINIMAL}
          />
        </Popover>
      </div>
    );
  }

  layerSettings(layer) {
    return (
      <Menu>
        <MenuItem iconName="tint" text="Theme...">
          {
            layerThemes.map((theme, idx) => (
              <MenuItem
                key={idx}
                text={theme.name}
                label={
                  <span
                    className={classNames(Classes.iconClass("tint"), Classes.ICON_STANDARD)}
                    style={styleFromTheme(theme)}
                  />
                }
                onClick={() => this.props.onLayerStyleChange(layer.id, "THEME", idx)}
              />
            ))
          }
        </MenuItem>
        <MenuItem iconName="info-sign" text="Info">
          <MenuItem text={`Description: ${layer.metadata.description}`} disabled={true} />
          <MenuItem text={`Attribution: ${layer.metadata.attribution}`} disabled={true} />
        </MenuItem>
        <MenuDivider />
        <MenuItem
          iconName="cross"
          text="Remove"
          onClick={() => this.props.layerClose(layer.id)}
        />
      </Menu>
    )
  }

  filterActive(layer) {
    return _.some(layer.filter, attr => (_.size(attr.categories) > 0) || attr.notApplicable);
  }
}

const toggleOnPredicate = (node, predicate) => (predicate ? node.onCollapse() : node.onExpand());

const recursivelyUpdateExpansionState = (nodes) => {
  if (nodes) {
    nodes.forEach(node => {
      node.updateExpansion();
      recursivelyUpdateExpansionState(node.childNodes);
    });
  }
}

const styleFromTheme = (theme) => ({
  color: theme.line,
  backgroundColor: theme.fill,
});



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
