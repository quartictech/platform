import React from "react";
import naturalsort from "javascript-natural-sort";
import Pane from "../Pane";
import { defaultTitle, curatedTitles } from "./behaviors";
const $ = require("jquery");
const _ = require("underscore");

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
      <Pane title={title} visible={visible} onClose={this.props.onClose}>
        {
          (loaded)
            ? null
            : <div className="ui active indeterminate massive text loader">Loading...</div>
        }

        {
          (histogramEnabled(entityIds))
            ? <Histograms histograms={histograms} />
            : <NonHistograms featureAttributes={attributes} layer={singleLayer(entityIds, layers)} />
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

const NonHistograms = ({ featureAttributes, layer }) => {
  const behavior = getBehavior(layer);
  return (
    <div>
      <Media featureAttributes={featureAttributes} behavior={behavior} />
      <AttributesTable
        featureAttributes={featureAttributes}
        behavior={behavior}
        order={behavior.isAnythingBlessed ? behavior.blessedAttributeOrder : behavior.unblessedAttributeOrder}
      />
      <div>
        <div className="ui accordion" ref={x => $(x).accordion()}>
          <div className="title">
            <i className="dropdown icon"></i>
            More attributes
          </div>

          <div className="content">
            <AttributesTable
              featureAttributes={featureAttributes}
              behavior={behavior}
              order={behavior.isAnythingBlessed ? behavior.unblessedAttributeOrder : behavior.blessedAttributeOrder}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

const Media = ({ featureAttributes, behavior }) => {
  if (behavior.imageUrlKey) {
    if (_.size(featureAttributes) === 1) {
      return (
        <Image url={_.values(featureAttributes)[0][behavior.imageUrlKey]} />
      );
    }

    return (
      <table className="ui very basic very compact small fixed table">
        <tbody>
          <tr>
            {_.map(featureAttributes, (attrs, id) =>
              <td key={id}><Image url={attrs[behavior.imageUrlKey]} /></td>
            )}
          </tr>
        </tbody>
      </table>
    );
  }

  return null;
};

const Image = ({ url }) => {
  if (!url) {
    return null;
  }

  const isVideo = url.endsWith(".mp4");

  return (isVideo ?
    <video className="ui fluid image" autoPlay loop src={url} />
    : <img role="presentation" className="ui fluid image" src={url} />);
};

const AttributesTable = ({ featureAttributes, behavior, order }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable definition table">
      {
        (_.size(featureAttributes) > 1) &&
          <thead>
            <tr>
              <th />
              {_.map(featureAttributes, (attrs, id) => <th key={id}>{behavior.title(attrs)}</th>)}
            </tr>
          </thead>
      }
      <tbody>
        {order
          .filter(key => _.some(_.values(featureAttributes), attrs => isAttributeDisplayable(key, attrs)))
          .map(key => (
            <tr key={key}>
              <td className="right aligned">{key}</td>
              {_.map(featureAttributes, (attrs, id) => <td key={id}>{attrs[key]}</td>)}
            </tr>
          ))
        }
      </tbody>
    </table>
  </div>
);

const isAttributeDisplayable = (key, attributes) =>
  !key.startsWith("_") && (key in attributes) && (String(attributes[key]).trim() !== "");

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
