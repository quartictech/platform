import React from "react";
import naturalsort from "javascript-natural-sort";
import Pane from "../Pane";
import { defaultBehavior, curatedBehaviors } from "./behaviors";
const $ = require("jquery");
const _ = require("underscore");

class SelectionPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    const layers = this.props.layers;
    const featureIds = this.props.selectedFeaturedIds;
    const info = this.props.selectionInfo;
    const featureAttributes = info.data.featureAttributes;

    if (_.size(featureIds) === 0) {
      return null;
    }

    const visible = true; // TODO
    const showHistograms = false; // TODO
    const loaded = (info.lifecycleState === "INFO_LOADED");
    const title = "TODO"; // TODO

    // const title = (filteredFeatures.length > 1)
    //   ? `${filteredFeatures.length} features selected`
    //   : getTitle(filteredFeatures[0].layer.metadata.name, filteredFeatures[0].properties);



    return (
      <Pane title={title} visible={visible} onClose={this.props.onClose}>
        {
          (loaded)
            ? null
            : <div className="ui active indeterminate massive text loader">Loading...</div>
        }

        {
          showHistograms
            ? <Histograms histograms={info.data.histograms} />
            : <NonHistograms
              layers={layers}
              featureIds={featureIds}
              featureAttributes={info.data.featureAttributes}
              />
        }
      </Pane>
    );
  }
}

const NonHistograms = ({ layers, featureIds, featureAttributes }) => {
  const layerId = _.keys(featureIds)[0];
  const layerName = layers[layerId].metadata.name;
  const attributeKeys = layers[layerId].attributeSchema.attributes;
  const behavior = getBehavior(layerName, attributeKeys);

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

const Image = ({ url }) => (
  <img className="ui fluid image" src={url} alt={url} />
);

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
  (key !== "_id") && (key in attributes) && (String(attributes[key]).trim() !== "");

const Histograms = ({ histograms }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable table">
      {
        _.chain(histograms)
          .sort((a, b) => naturalsort(a.property, b.property))
          .map(histogram =>
            <AttributeHistogram
              key={histogram.property}
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
        {histogram.property}
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

const displayMode = (features) => {
  const numUniqueLayers = _.chain(features).map(f => f.layer.id).uniq().size().value();
  const numFeatures = features.length;

  if (numFeatures === 1) {
    return "BASEBALL";
  }
  if (numFeatures < 5 && numUniqueLayers === 1) {
    return "SIDE_BY_SIDE";
  }
  return "HISTOGRAM";
};

const getBehavior = (layerName, attributeKeys) => {
  const b = (layerName in curatedBehaviors) ? curatedBehaviors[layerName] : defaultBehavior;
  return {
    title: (attributes) => b.title(attributes),
    imageUrlKey: b.imageUrl,
    isAnythingBlessed: b.blessed.length > 0,
    // In the specified order
    blessedAttributeOrder: b.blessed.filter(k => k in attributeKeys),
    // Find all other attributes, and then natural-sort for convenience
    unblessedAttributeOrder: _.keys(attributeKeys).filter(k => (b.blessed.indexOf(k) === -1)).sort(naturalsort),
  };
};

export default SelectionPane;
