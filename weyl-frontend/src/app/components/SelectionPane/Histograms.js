import React from "react";
import naturalsort from "javascript-natural-sort";
import * as _ from "underscore";

import {
  Tree,
} from "@blueprintjs/core";

// TODO: blessed vs. non-blessed
class Histograms extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super();
    this.state = {
      activeAttribute: null,
      nodes: [],
      histograms: null,
    };
  }

  componentWillReceiveProps(nextProps) {
    // We maintain the state for the tree, which we have to keep in-sync with props
    if (nextProps.histograms !== this.state.histograms) {
      this.setState({
        nodes: this.attributeNodes(nextProps.histograms.toJS()),
        histograms: nextProps.histograms,
      });
    }
  }

  render() {
    return (
      <Tree
        contents={this.state.nodes}
        onClick={n => n.onClick()}
        onNodeExpand={n => n.onExpand()}
        onNodeCollapse={n => n.onCollapse()}
      />
    );
  }

  setState(nextState) {
    this.state = { ...this.state, ...nextState };
    recursivelyUpdateExpansionState(this.state.nodes);  // Get the nodes to update their own isExpanded values
    super.setState(this.state);
  }

  attributeNodes(histograms) {
    return _.chain(histograms)
      .sort((a, b) => naturalsort(a.attribute, b.attribute))
      .map(h => {
        const node = {
          iconName: "property",
          id: h.attribute,
          label: <small>{h.attribute}</small>,
          childNodes: this.bucketNodes(h.buckets),
        };
        node.onClick = () => toggleOnPredicate(node, this.state.activeAttribute === h.attribute);
        node.onExpand = () => this.setState({ activeAttribute: h.attribute });
        node.onCollapse = () => this.setState({ activeAttribute: null });
        node.updateExpansion = () => (node.isExpanded = (this.state.activeAttribute === h.attribute));
        return node;
      })
      .value();
  }

  bucketNodes(buckets) {
    const bucketMax = _.max(buckets, b => b.count).count;
    return [
      ..._.chain(buckets)
        .sort((a, b) => naturalsort(a.value, b.value))  // Fall back to alphabetical
        .sort((a, b) => b.count - a.count)              // Highest count first
        .map(b => this.valueNode(b.value, b.count, bucketMax))
        .value(),
    ];
  }

  valueNode(label, count, maxValue) {
    const barLength = maxValue > 0 ? (count / maxValue) : 0;
    return {
      id: label,
      label: <small>{label}</small>,
      secondaryLabel: (
        <span>
          <svg width="100" height="10">
            <g transform="translate(0,0)">
              <rect fill="#f5f8fa" width={90 * barLength} height="20"></rect>
            </g>
          </svg>
          {count}
        </span>
      ),
      onClick: () => {},
      onExpand: () => {},
      onCollapse: () => {},
      updateExpansion: () => {},
    };
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

export default Histograms;
