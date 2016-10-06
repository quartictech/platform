import React from "react";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");
const _ = require("underscore");

import styles from "./styles.css";
import { defaultBehavior, curatedBehaviors } from "./behaviors";

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function
  renderInner() {
    const filteredSelection = this.props.selection
      .filter(s => s.layer.visible);  // TODO: take account of actual filtering

    if (filteredSelection.length === 0) {
      return null;
    }

    return (
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
    );
  }

  render() {
    return (
      <div className={styles.selectionView}>
        {this.renderInner()}
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
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  return (hasImageUrl(layerName)) ? (
    <div className="ui segment">
      <img
        className="ui fluid image"
        src={properties[getImageUrl(layerName)]}
        alt={properties[getImageUrl(layerName)]}
      />
    </div>
  ) : <div></div>;
};

const BlessedProperties = ({ selection }) => {
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  const numUniqueLayers = _.chain(selection).map(s => s.layer.id).uniq().size().value();
  const numFeatures = selection.length;

  return (
    <div className="ui segment">
      <PropertiesTable
        selection={selection}
        order={
          isAnythingBlessed(layerName)
            ? getBlessedPropertyOrder(layerName, selection[0].properties) // TODO: union properties
            : getUnblessedPropertyOrder(layerName, selection[0].properties) // TODO: union properties
        }
      />
    </div>
  );
};

const UnblessedProperties = ({ selection }) => {
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  // TODO
  return <div></div>;

  // return (
  //   <div className="ui secondary segment">
  //     <PropertiesTable
  //       properties={properties}
  //       order={
  //         isAnythingBlessed(layerName)
  //           ? getUnblessedPropertyOrder(layerName, properties)
  //           : getBlessedPropertyOrder(layerName, properties)
  //       }
  //     />
  //   </div>
  // );
};

const PropertiesTable = ({ selection, order }) => (
  <table className="ui very basic celled very compact small fixed selectable table">
    {
      (selection.length > 1) &&
        <thead>
          <tr>
            <th />
            {selection.map(s =>
              <th key={s.properties["_id"]}>{getTitle(s.layer.metadata.name, s.properties)}</th>  // eslint-disable-line dot-notation
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
              <td key={s.properties["_id"]}>{s.properties[key]}</td>  // eslint-disable-line dot-notation
            )}
          </tr>
        )
      }
    </tbody>
  </table>
);

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
