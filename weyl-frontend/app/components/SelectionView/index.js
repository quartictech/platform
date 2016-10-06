import React from "react";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");
const _ = require("underscore");

import styles from "./styles.css";
import { defaultBehavior, curatedBehaviors } from "./behaviors";

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    const filteredSelection = this.props.selection
      .filter(s => s.layer.visible);  // TODO: take account of actual filtering

    if (filteredSelection.length === 0) {
      return null;
    }

    return (
      <div className={styles.selectionView}>
        <div className={styles.innerSelectionView}>
          <div className="ui raised fluid card">
            <div className="content">
              <Header selection={filteredSelection} onClose={this.props.onClose} />
              <Media selection={filteredSelection} />
              <BlessedProperties selection={filteredSelection} />
            </div>

            <div className="extra content">
              <div className="ui accordion" ref={x => $(x).accordion()}>
                <div className="title">
                  <i className="dropdown icon"></i>
                  More properties
                </div>

                <div className="content">
                  <UnblessedProperties selection={filteredSelection} />
                </div>

              </div>
            </div>

          </div>
        </div>
      </div>
    );
  }
}

const Header = ({ selection, onClearSelectionClick }) => (
  <div className="header">
    <a onClick={onClearSelectionClick}>
      <i className="icon close"></i>
    </a>
    {
      (selection.length > 1)
        ? `${selection.length} features selected`
        : getTitle(selection[0].layer.metadata.name, selection[0].properties)
    }
  </div>
);

const Media = ({ selection }) => {
  if (displayMode(selection) === "AGGREGATE") {
    return <div className="ui segment">TODO: aggregation</div>;
  }

  // TODO: given the above check, features should be homogeneous - need to generalise this
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  if (hasImageUrl(layerName)) {
    if (displayMode(selection) === "BASEBALL") {
      return (
        <div className="ui segment">
          <Image url={properties[getImageUrl(layerName)]} />
        </div>
      );
    }

    return (
      <div className="ui segment">
        <table className="ui very basic very compact small fixed table">
          <tr>
            {selection.map(s =>
              <td key={s.properties["_id"]}><Image url={s.properties[getImageUrl(layerName)]} /></td>    // eslint-disable-line dot-notation
            )}
          </tr>
        </table>
      </div>
    );
  }
};

const Image = ({ url }) => (
  <img className="ui fluid image" src={url} alt={url} />
);

const BlessedProperties = ({ selection }) => {
  if (displayMode(selection) === "AGGREGATE") {
    return <div className="ui segment">TODO: aggregation</div>;
  }

  // TODO: given the above check, features should be homogeneous - need to generalise this
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;
  return (
    <div className="ui segment">
      <PropertiesTable
        selection={selection}
        order={
          isAnythingBlessed(layerName)
            ? getBlessedPropertyOrder(layerName, properties)
            : getUnblessedPropertyOrder(layerName, properties)
        }
      />
    </div>
  );
};

const UnblessedProperties = ({ selection }) => {
  if (displayMode(selection) === "AGGREGATE") {
    return <div className="ui segment">TODO: aggregation</div>;
  }

  // TODO: given the above check, features should be homogeneous - need to generalise this
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;
  return (
    <div className="ui segment">
      <PropertiesTable
        selection={selection}
        order={
          isAnythingBlessed(layerName)
            ? getUnblessedPropertyOrder(layerName, properties)
            : getBlessedPropertyOrder(layerName, properties)
        }
      />
    </div>
  );
};

const PropertiesTable = ({ selection, order }) => (
  <table className="ui very basic celled very compact small fixed selectable table">
    {
      (selection.length > 1) &&
        <thead>
          <tr>
            <th />
            {selection.map(s =>
              <th key={s.properties["_id"]}>{getTitle(s.layer.metadata.name, s.properties)}</th>    // eslint-disable-line dot-notation
            )}
          </tr>
        </thead>
    }
    <tbody>
      {order
        .filter(key => _.some(selection, s => isPropertyDisplayable(key, s.properties)))
        .map(key =>
          <tr key={key}>
            <td className="right aligned">
              <div className="ui sub header">{key}</div>
            </td>
            {selection.map(s =>
              <td key={s.properties["_id"]}>{s.properties[key]}</td>    // eslint-disable-line dot-notation
            )}
          </tr>
        )
      }
    </tbody>
  </table>
);

const displayMode = (selection) => {
  const numUniqueLayers = _.chain(selection).map(s => s.layer.id).uniq().size().value();
  const numFeatures = selection.length;

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
