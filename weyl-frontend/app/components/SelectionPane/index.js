import React from "react";
import {
  Classes,
  Spinner,
} from "@blueprintjs/core";
import * as _ from "underscore";
import naturalsort from "javascript-natural-sort";

import Pane from "../Pane";
import NonHistograms from "./NonHistograms";
import Histograms from "./Histograms";
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
        { (loaded) ?
          <SelectionView
            entityIds={entityIds}
            histograms={histograms}
            attributes={attributes}
            layers={layers}
          />
        :
          <div style={{ width: "100%", textAlign: "center" }}>
            <Spinner className={Classes.LARGE} />
          </div>
        }
      </Pane>
    );
  }
}

const SelectionView = ({ entityIds, histograms, attributes, layers }) => {
  if (histogramEnabled(entityIds)) {
    return <Histograms histograms={histograms} />;
  }
  return (
    <NonHistograms
      featureAttributes={attributes}
      behavior={getBehavior(singleLayer(entityIds, layers))}
    />
  );
};

// entityIds is an object { layerId -> [entityIds] }
const histogramEnabled = (entityIds) =>
  (_.size(entityIds) > 1 || numEntities(entityIds) > 4);

const numEntities = (entityIds) => _.size(_.flatten(_.values(entityIds)));

const singleLayer = (entityIds, layers) => layers[_.keys(entityIds)[0]];

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
