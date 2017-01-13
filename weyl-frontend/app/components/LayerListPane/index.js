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
  Position,
  Tree,
  Tooltip,
} from "@blueprintjs/core";

import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
import * as _ from "underscore";

import { layerThemes } from "../../themes";
import Pane from "../Pane";

import { DateRangePicker } from "./DateRangePicker";
import { formatDateTime } from "../../utils/time";

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
        visible={this.props.visible}
        onClose={this.props.onClose}
      >
        <Tree
          contents={this.state.nodes}
          onNodeExpand={n => n.onExpand()}
          onNodeCollapse={n => n.onCollapse()}
          onNodeClick={(n, p, e) => (e.target.nodeName !== "BUTTON") && n.onClick()} // Because the buttons propagate events
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
          layer.dynamicSchema.attributes,
          layer.filter,
          layer.stats ? layer.stats.attributeStats : {},
          (k, v) => this.props.onToggleValueVisible(layer.id, k, v),
          (k, startTime, endTime) => this.props.onApplyTimeRangeFilter(layer.id, k, startTime, endTime)
        ),
      };
      node.onClick = () => toggleOnPredicate(node, this.state.activeLayerId === layer.id);
      node.onExpand = () => this.setState({ activeLayerId: layer.id, activeAttribute: null });
      node.onCollapse = () => this.setState({ activeLayerId: null, activeAttribute: null });
      node.updateExpansion = () => (node.isExpanded = (this.state.activeLayerId === layer.id));
      return node;
    });
  }

  attributeNodes(attributes, filter, attributeStats, onValueClick, applyTimeRangeFilter) {
    return _.chain(attributes)
      .keys()
      .sort(naturalsort)
      .map(k => {
        const node = this.attributeNode(k, attributes[k], attributeStats[k], filter[k], onValueClick, applyTimeRangeFilter);
        node.onClick = () => toggleOnPredicate(node, this.state.activeAttribute === k);
        node.onExpand = () => this.setState({ activeAttribute: k });
        node.onCollapse = () => this.setState({ activeAttribute: null });
        node.updateExpansion = () => (node.isExpanded = (this.state.activeAttribute === k));
        return node;
      })
      .value();
  }

  attributeNode(attribute, attributeInfo, attributeStats, filter, onValueClick, applyTimeRangeFilter) {
    if (attributeInfo.type === "TIMESTAMP") {
      return {
        iconName: "time",
        id: attribute,
        label: <small>{attribute}</small>,
        secondaryLabel: (
          <Tooltip content={this.timeRangeFilterTooltip(filter)}>
            <Popover
              content={
                <DateRangePicker
                  startTime={filter && filter.timeRange ? filter.timeRange.startTime : null}
                  endTime={filter && filter.timeRange ? filter.timeRange.endTime : null}
                  minTime={attributeStats ? attributeStats.minimum : null}
                  maxTime={attributeStats ? attributeStats.maximum : null}
                  onApply={(startTime, endTime) => applyTimeRangeFilter(attribute, startTime, endTime)}
                />}
              position={Position.RIGHT_TOP}
            >
              <Button
                iconName="filter"
                className={Classes.MINIMAL}
                intent={filter && filter.timeRange ? Intent.WARNING : Intent.NONE}
              />
            </Popover>
          </Tooltip>
        ),
      };
    } else if (attributeInfo.categories) {
      return {
        iconName: "th-list",
        id: attribute,
        label: <small>{attribute}</small>,
        childNodes: this.attributeCategoryNodes(
          attributeInfo.categories,
          filter,
          (v) => onValueClick(attribute, v),
        ),
      };
    }
    return {
      iconName: "property",
      id: attribute,
      label: <small>{attribute}</small>,
    };
  }

  timeRangeFilterTooltip(filter) {
    if (filter && filter.timeRange) {
      const startRange = filter.timeRange.startTime ? formatDateTime(filter.timeRange.startTime) : "";
      const endRange = filter.timeRange.endTime ? formatDateTime(filter.timeRange.endTime) : "";
      return `${startRange} → ${endRange}`;
    }
    return "No filter set";
  }

  attributeCategoryNodes(categories, filter, onClick) {
    // Filter for this attribute may be undefined, initially
    return [
      this.attributeCategoryNode("< N/A >", !filter || !filter.notApplicable, () => onClick()),  // Note no argument to callback
      ..._.chain(categories)
        .sort(naturalsort)
        .map(c => this.attributeCategoryNode(c, !filter || !_.contains(filter.categories, c), () => onClick(c)))
        .value(),
    ];
  }

  attributeCategoryNode(label, enabled, onClick) {
    return {
      id: label,
      label: <small>{label}</small>,
      secondaryLabel: (
        <Button
          iconName={enabled ? "eye-open" : "eye-off"}
          onClick={onClick}
          className={Classes.MINIMAL}
          intent={enabled ? Intent.PRIMARY : Intent.NONE}
        />
      ),
      onClick,
      onExpand: () => {},
      onCollapse: () => {},
      updateExpansion: () => {},
    };
  }

  layerButtons(layer) {
    const filterBasedIntent = this.filterActive(layer) ? Intent.WARNING : Intent.PRIMARY;
    return (
      <div className={Classes.BUTTON_GROUP}>
        <Button
          iconName={layer.visible ? "eye-open" : "eye-off"}
          onClick={() => this.props.layerToggleVisible(layer.id)}
          className={Classes.MINIMAL}
          intent={layer.visible ? filterBasedIntent : Intent.NONE}
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
                    className={classNames(Classes.iconClass("layers"), Classes.ICON_STANDARD)}
                    style={styleFromTheme(theme)}
                  />
                }
                onClick={() => this.props.onLayerStyleChange(layer.id, "THEME", idx)}
              />
            ))
          }
        </MenuItem>
        <MenuItem iconName="tint" text="Colour by...">
          {
            _.keys(layer.dynamicSchema.attributes)
              .filter(k => layer.dynamicSchema.attributes[k].type === "NUMERIC")
              .sort(naturalsort)
              .map(k =>
                <MenuItem
                  key={k}
                  text={k}
                  iconName="property"
                  onClick={() => this.props.onLayerStyleChange(layer.id, "ATTRIBUTE", k)}
                />
              )
          }
        </MenuItem>
        <MenuItem iconName="info-sign" text="Info">
          <MenuItem text={`Description: ${layer.metadata.description}`} disabled />
          <MenuItem text={`Attribution: ${layer.metadata.attribution}`} disabled />
        </MenuItem>
        <MenuDivider />
        <MenuItem
          iconName="trash"
          text="Remove"
          intent={Intent.DANGER}
          onClick={() => this.props.layerClose(layer.id)}
        />
      </Menu>
    );
  }

  filterActive(layer) {
    return _.some(layer.filter, attr => (_.size(attr.categories) > 0) || attr.notApplicable || attr.timeRange);
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
};

const styleFromTheme = (theme) => ({
  color: theme.line,
  backgroundColor: theme.fill,
});

export default LayerListPane;
