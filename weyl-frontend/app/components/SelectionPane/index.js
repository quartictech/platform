import React from "react";
import naturalsort from "javascript-natural-sort";
import Pane from "../Pane";
import { defaultBehavior, curatedBehaviors } from "./behaviors";
const $ = require("jquery");
const _ = require("underscore");

class SelectionPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    const layers = this.props.layers;
    const info = this.props.selectionInfo;
    const attributes = info.data.attributes;

    return null;

    // TODO: this is silly
    if (!attributes || attributes.length === 0) {
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
            ? null
            : <Media attributes={attributes} />
        }
        {
          showHistograms
            ? <Histograms histograms={info.data.histograms} />
            : <BlessedAttributes attributes={attributes} />
        }

        {
          showHistograms ? null : (
            <div>
              <div className="ui accordion" ref={x => $(x).accordion()}>
                <div className="title">
                  <i className="dropdown icon"></i>
                  More properties
                </div>

                <div className="content">
                  <UnblessedAttributes attributes={attributes} />
                </div>
              </div>
            </div>
          )
        }
      </Pane>
    );
  }
}

const Media = ({ attributes }) => {
  const properties = features[0].properties;
  const layerName = features[0].layer.metadata.name;

  if (hasImageUrl(layerName)) {
    if (displayMode(features) === "BASEBALL") {
      return (
        <Image url={properties[getImageUrl(layerName)]} />
      );
    }

    return (
      <table className="ui very basic very compact small fixed table">
        <tbody>
          <tr>
            {features.map(f =>
              <td key={f.attributes["_id"]}><Image url={f.properties[getImageUrl(layerName)]} /></td>    // eslint-disable-line dot-notation
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

const Histograms = ({ histograms }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable table">
      {
        _.chain(histograms)
          .sort((a, b) => naturalsort(a.property, b.property))
          .map(histogram =>
            <PropertyHistogram
              key={histogram.property}
              histogram={histogram}
            />
          )
          .value()
      }
    </table>
  </div>
);

const PropertyHistogram = ({ histogram }) => (
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

const BlessedAttributes = ({ features, layerName }) => (
  <PropertiesTable
    features={features}
    order={
      isAnythingBlessed(layerName)
        ? getBlessedPropertyOrder(layerName, _.values(features)[0].attributes)
        : getUnblessedPropertyOrder(layerName, _.values(features)[0].attributes)
    }
  />
);

const UnblessedAttributes = ({ features, layerName }) => (
  <PropertiesTable
    features={features}
    order={
      isAnythingBlessed(layerName)
        ? getUnblessedPropertyOrder(layerName, _.values(features)[0].attributes)
        : getBlessedPropertyOrder(layerName, _.values(features)[0].attributes)
    }
  />
);

const AttributesTable = ({ features, order }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable definition table">
      {
        (features.length > 1) &&
          <thead>
            <tr>
              <th />
              {features.map(f =>
                <th key={f.attributes["_id"]}>{getTitle(f.layer.metadata.name, f.attributes)}</th>    // eslint-disable-line dot-notation
              )}
            </tr>
          </thead>
      }
      <tbody>
        {order
          .filter(key => _.some(features, f => isPropertyDisplayable(key, f.attributes)))
          .map(key =>
            <tr key={key}>
              <td className="right aligned">{key}</td>
              {features.map(f =>
                <td key={f.attributes["_id"]}>{f.attributes[key]}</td>    // eslint-disable-line dot-notation
              )}
            </tr>
          )
        }
      </tbody>
    </table>
  </div>
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

const isAttributeDisplayable = (key, attributes) =>
  (key !== "_id") && (key in attributes) && (String(attributes[key]).trim() !== "");

const getTitle = (layerName, attributes) =>
  getBehavior(layerName).title(attributes);

const hasImageUrl = (layerName) =>
  ("imageUrl" in getBehavior(layerName));

const getImageUrl = (layerName) =>
  getBehavior(layerName).imageUrl;

const isAnythingBlessed = (layerName) =>
  getBehavior(layerName).blessed.length > 0;

// In the specified order
const getBlessedAttributeOrder = (layerName, attributes) =>
  getBehavior(layerName).blessed.filter(k => k in attributes);

// Find all other attributes, and then natural-sort for convenience
const getUnblessedAttributeOrder = (layerName, attributes) => {
  const behavior = getBehavior(layerName);
  return Object.keys(attributes)
    .filter(k => (behavior.blessed.indexOf(k) === -1))
    .sort(naturalsort);
};

const getBehavior = (layerName) =>
  ((layerName in curatedBehaviors) ? curatedBehaviors[layerName] : defaultBehavior);

export default SelectionPane;
