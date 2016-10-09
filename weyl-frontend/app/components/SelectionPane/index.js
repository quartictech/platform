import React from "react";
import naturalsort from "javascript-natural-sort";
import Pane from "../Pane";
import { defaultBehavior, curatedBehaviors } from "./behaviors";
const $ = require("jquery");
const _ = require("underscore");

class SelectionPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    // TODO: move this into reducer
    const filteredFeatures = this.props.selection.features
      .filter(f => f.layer.visible);  // TODO: take account of actual filtering

    if (filteredFeatures.length === 0) {
      return null;
    }

    const showAggregates = (displayMode(filteredFeatures) === "AGGREGATE");

    const title = (filteredFeatures.length > 1)
      ? `${filteredFeatures.length} features selected`
      : getTitle(filteredFeatures[0].layer.metadata.name, filteredFeatures[0].properties);

    const visible = true;

    return (
      <Pane title={title} visible={visible} onClose={this.props.onClose}>
        {
          showAggregates
            ? null
            : <Media features={filteredFeatures} />
        }
        {
          showAggregates
            ? <Aggregates aggregates={this.props.selection.aggregates} />
            : <BlessedProperties features={filteredFeatures} />
        }

        {
          showAggregates ? null : (
            <div>
              <div className="ui accordion" ref={x => $(x).accordion()}>
                <div className="title">
                  <i className="dropdown icon"></i>
                  More properties
                </div>

                <div className="content">
                  <UnblessedProperties features={filteredFeatures} />
                </div>
              </div>
            </div>
          )
        }
      </Pane>
    );
  }
}

const Media = ({ features }) => {
  // We can assume properties are homogeneous
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
              <td key={f.properties["_id"]}><Image url={f.properties[getImageUrl(layerName)]} /></td>    // eslint-disable-line dot-notation
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

const Aggregates = ({ aggregates }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    {
      (aggregates.lifecycleState === "AGGREGATES_LOADING")
        ? <div className="ui active indeterminate massive text loader">Loading...</div>
        : null
    }

    <table className="ui celled very compact small fixed selectable table">
      {
        _.chain(aggregates.data)
          .sort((a, b) => naturalsort(a.property, b.property))
          .map(histogram =>
            <AggregatesProperty
              key={histogram.property}
              histogram={histogram}
            />
          )
          .value()
      }
    </table>
  </div>
);

const AggregatesProperty = ({ histogram }) => (
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

const BlessedProperties = ({ features }) => {
  // We can assume properties are homogeneous
  const properties = features[0].properties;
  const layerName = features[0].layer.metadata.name;
  return (
    <PropertiesTable
      features={features}
      order={
        isAnythingBlessed(layerName)
          ? getBlessedPropertyOrder(layerName, properties)
          : getUnblessedPropertyOrder(layerName, properties)
      }
    />
  );
};

const UnblessedProperties = ({ features }) => {
  // We can assume properties are homogeneous
  const properties = features[0].properties;
  const layerName = features[0].layer.metadata.name;
  return (
    <PropertiesTable
      features={features}
      order={
        isAnythingBlessed(layerName)
          ? getUnblessedPropertyOrder(layerName, properties)
          : getBlessedPropertyOrder(layerName, properties)
      }
    />
  );
};

const PropertiesTable = ({ features, order }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable definition table">
      {
        (features.length > 1) &&
          <thead>
            <tr>
              <th />
              {features.map(f =>
                <th key={f.properties["_id"]}>{getTitle(f.layer.metadata.name, f.properties)}</th>    // eslint-disable-line dot-notation
              )}
            </tr>
          </thead>
      }
      <tbody>
        {order
          .filter(key => _.some(features, f => isPropertyDisplayable(key, f.properties)))
          .map(key =>
            <tr key={key}>
              <td className="right aligned">{key}</td>
              {features.map(f =>
                <td key={f.properties["_id"]}>{f.properties[key]}</td>    // eslint-disable-line dot-notation
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
  return "AGGREGATE";
};

const isPropertyDisplayable = (key, properties) =>
  (key !== "_id") && (key in properties) && (String(properties[key]).trim() !== "");

const getTitle = (layerName, properties) =>
  getBehavior(layerName).title(properties);

const hasImageUrl = (layerName) =>
  ("imageUrl" in getBehavior(layerName));

const getImageUrl = (layerName) =>
  getBehavior(layerName).imageUrl;

const isAnythingBlessed = (layerName) =>
  getBehavior(layerName).blessed.length > 0;

// In the specified order
const getBlessedPropertyOrder = (layerName, properties) =>
  getBehavior(layerName).blessed.filter(k => k in properties);

// Find all other properties, and then natural-sort for convenience
const getUnblessedPropertyOrder = (layerName, properties) => {
  const behavior = getBehavior(layerName);
  return Object.keys(properties)
    .filter(k => (behavior.blessed.indexOf(k) === -1))
    .sort(naturalsort);
};

const getBehavior = (layerName) =>
  ((layerName in curatedBehaviors) ? curatedBehaviors[layerName] : defaultBehavior);

export default SelectionPane;
