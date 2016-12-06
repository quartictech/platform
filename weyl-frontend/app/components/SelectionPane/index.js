import React from "react";
import {
  Button,
  Classes,
  Collapse,
  Overlay,
  Spinner,
} from "@blueprintjs/core";
import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
import * as $ from "jquery";
import * as _ from "underscore";

import Pane from "../Pane";
import NonHistograms from "./NonHistograms";
import { defaultTitle, curatedTitles } from "./behaviors";

class SelectionPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    const layers = this.props.layers;
    const entityIds = this.props.selection.ids;
    const histograms = this.props.histograms.data;
    const attributes = this.props.attributes.data;

    // TODO: reconcile with visibility
    if (_.size(entityIds) === 0) {
      return null;
    }
    const visible = true; // TODO
    const loaded = (histogramEnabled(entityIds) ? this.props.histograms : this.props.attributes).seqNum === this.props.selection.seqNum;

    // TODO: depluralise appropriately
    const title = (numEntities(entityIds) > 1 || _.size(attributes) === 0)
      ? `${numEntities(entityIds)} entities selected`
      : getBehavior(singleLayer(entityIds, layers)).title(_.values(attributes)[0]);

    return (
      <Pane
        title={title}
        iconName="properties"
        visible={visible}
        onClose={this.props.onClose}
      >
        {
          (histogramEnabled(entityIds))
            ? <Histograms histograms={histograms} />
            : (
              <NonHistograms
                featureAttributes={attributes}
                behavior={getBehavior(singleLayer(entityIds, layers))}
              />
            )
        }
      </Pane>
    );
  }
}

// entityIds is an object { layerId -> [entityIds] }
const histogramEnabled = (entityIds) =>
  (_.size(entityIds) > 1 || numEntities(entityIds) > 4);

const numEntities = (entityIds) => _.size(_.flatten(_.values(entityIds)));

const singleLayer = (entityIds, layers) => layers[_.keys(entityIds)[0]];

// TODO: blessed vs. non-blessed
const Histograms = ({ histograms }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable table">
      {
        _.chain(histograms)
          .sort((a, b) => naturalsort(a.attribute, b.attribute))
          .map(histogram =>
            <AttributeHistogram
              key={histogram.attribute}
              histogram={histogram}
            />
          )
          .value()
      }
    </table>
  </div>
);

const AttributeHistogram = ({ histogram }) => (
  <tbody className="ui accordion" ref={x => $(x).accordion()}>
    <tr className="title">
      <td style={{ fontWeight: "bold" }}>
        <i className="dropdown icon"></i>
        {histogram.attribute}
      </td>
    </tr>

    <tr className="content">
      <td>
        <table className="ui celled very compact small fixed selectable definition table">
          <tbody>
            {
              _.chain(histogram.buckets)
                .sort((a, b) => naturalsort(a.value, b.value))  // Fall back to alphabetical
                .sort((a, b) => b.count - a.count)              // Highest count first
                .map(bucket =>
                  <tr key={bucket.value}>
                    <td className="right aligned">{bucket.value}</td>
                    <td>{bucket.count}</td>
                  </tr>
                )
                .value()
            }
          </tbody>
        </table>
      </td>
    </tr>
  </tbody>
);

const getBehavior = (layer) => {
  const layerName = layer.metadata.name;
  const schema = layer.attributeSchema;
  const attributeKeys = schema.attributes;
  const nonCuratedTitle = (schema.titleAttribute
    ? ((x) => ((schema.titleAttribute in x) ? x[schema.titleAttribute] : "<< Unknown title >>"))
    : defaultTitle);
  return {
    title: (layerName in curatedTitles) ? curatedTitles[layerName] : nonCuratedTitle,
    imageUrlKey: schema.imageAttribute,
    isAnythingBlessed: schema.blessedAttributes.length > 0,
    // In the specified order
    blessedAttributeOrder: schema.blessedAttributes.filter(k => k in attributeKeys),
    // Find all other attributes, and then natural-sort for convenience
    unblessedAttributeOrder: _.keys(attributeKeys).filter(k => (schema.blessedAttributes.indexOf(k) === -1)).sort(naturalsort),
  };
};

export default SelectionPane;
