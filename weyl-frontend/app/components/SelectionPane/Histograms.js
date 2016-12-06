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
    };
  }

  componentWillReceiveProps(nextProps) {
    // We maintain the state for the tree, which we have to keep in-sync with props
    if (nextProps.histograms !== this.props.histograms) {
      this.setState({ nodes: this.attributeNodes(nextProps.histograms) });
    }
  }

  attributeNodes(histograms) {
    return _.chain(histograms)
      .sort((a, b) => naturalsort(a.attribute, b.attribute))
      .map(h => {
        const node = {
          iconName: "property",
          id: h.attribute,
          label: h.attribute,
          childNodes: this.bucketNodes(
            h.buckets
          ),
        };
        return node;
      })
      .value();
  }

  bucketNodes(buckets) {
    return [
      ..._.chain(buckets)
        .sort((a, b) => naturalsort(a.value, b.value))  // Fall back to alphabetical
        .sort((a, b) => b.count - a.count)              // Highest count first
        .map(b => this.valueNode(b.value, b.count))
        .value(),
    ];
  }

  valueNode(label, count) {
    return {
      id: label,
      label,
      secondaryLabel: (
        <span>
          {count}
        </span>
      ),
    };
  }

  nodeExpanded(node) {
    node.isExpanded = true; // eslint-disable-line no-param-reassign
    this.setState(this.state);
  }

  nodeCollapsed(node) {
    node.isExpanded = false; // eslint-disable-line no-param-reassign
    this.setState(this.state);
  }

  render() {
    return (<Tree
      contents={this.state.nodes}
      onNodeExpand={n => this.nodeExpanded(n)}
      onNodeCollapse={n => this.nodeCollapsed(n)}
    />);
  }
}

export default Histograms;
