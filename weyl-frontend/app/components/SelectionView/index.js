import React from "react";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");
const _ = require("underscore");

import styles from "./styles.css";
import { defaultBehavior, curatedBehaviors } from "./behaviors";

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    // TODO: move this into reducer
    const filteredFeatures = this.props.selection.features
      .filter(f => f.layer.visible);  // TODO: take account of actual filtering

    if (filteredFeatures.length === 0) {
      return null;
    }

    const showAggregates = (displayMode(filteredFeatures) === "AGGREGATE");

    return (
      <div className={styles.selectionView}>
        <div className={styles.innerSelectionView}>
          <div className="ui raised fluid card">
            <div className="content">
              <Header
                features={filteredFeatures}
                loading={this.props.selection.aggregates.lifecycleState === "AGGREGATES_LOADING"}
                onClose={this.props.onClose}
              />
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
            </div>

            {
              showAggregates
                ? null
                : <div className="extra content">
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
            }
          </div>
        </div>
      </div>
    );
  }
}

const Header = ({ features, loading, onClose }) => (
  <div className="header">
    {
      loading
        ? <div className="ui active indeterminate text loader">Loading...</div>
        : null
    }

    <a onClick={onClose}>
      <i className="icon close"></i>
    </a>
    {
      (features.length > 1)
        ? `${features.length} features selected`
        : getTitle(features[0].layer.metadata.name, features[0].properties)
    }
  </div>
);

const Media = ({ features }) => {
  // TODO: given the above check, features should be homogeneous - need to generalise this
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

const Aggregates = ({ aggregates }) => {
  return (
    <table className="ui celled very compact small fixed table">
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
  );
};

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
  // TODO: given the above check, features should be homogeneous - need to generalise this
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
  // TODO: given the above check, features should be homogeneous - need to generalise this
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

export default SelectionView;
